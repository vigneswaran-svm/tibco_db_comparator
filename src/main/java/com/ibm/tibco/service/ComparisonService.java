package com.ibm.tibco.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.tibco.dto.ComparisonRequest;
import com.ibm.tibco.dto.ComparisonResponse;
import com.ibm.tibco.dto.ServiceResult;
import com.ibm.tibco.model.db1.ComparatorConfigEntity;
import com.ibm.tibco.model.db1.ComparatorHistoryEntity;
import com.ibm.tibco.repository.ComparatorConfigRepository;
import com.ibm.tibco.repository.ComparatorHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ComparisonService {

    @PersistenceContext(unitName = "db1")
    private EntityManager msEntityManager;

    @PersistenceContext(unitName = "db2")
    private EntityManager soiEntityManager;

    @Autowired
    private ComparatorConfigRepository configRepository;

    @Autowired
    private ComparatorHistoryRepository historyRepository;

    @Autowired
    @Qualifier("db1TransactionManager")
    private PlatformTransactionManager db1TransactionManager;

    @Value("${comparison.fetch-size:500}")
    private int fetchSize;

    @Value("${comparison.batch-size:50}")
    private int batchSize;

    private final ObjectMapper objectMapper = new ObjectMapper();


    // ==================== Comparison ====================

    /**
     * No @Transactional here — each config is processed in its own transaction
     * via TransactionTemplate so one failure doesn't roll back others.
     */
    public ComparisonResponse compareTableRecords(ComparisonRequest request) {
        try {
            log.info("Starting comparison with request: {}", request);

            List<ComparatorConfigEntity> configs = fetchActiveConfigs(request);

            if (configs.isEmpty()) {
                log.warn("No active configurations found for the given filters");
                return ComparisonResponse.builder()
                        .status("WARNING")
                        .message("No active configurations found with status 'N'")
                        .build();
            }

            log.info("Found {} active configuration(s) to process", configs.size());

            int totalSuccessCount = 0;
            int totalFailureCount = 0;
            int totalMsRecords = 0;
            int totalSoiRecords = 0;
            List<String> tablesWithNoRecords = new ArrayList<>();
            List<String> tablesWithErrors = new ArrayList<>();
            Map<String, ServiceResult> serviceResultMap = new LinkedHashMap<>();

            for (ComparatorConfigEntity config : configs) {
                log.info("Processing config ID: {}, Service: {}, Table: {}",
                        config.getId(), config.getServiceId(), config.getTableName());

                ComparisonResult result = processSingleConfig(config);

                if (result.hasError) {
                    tablesWithErrors.add(config.getTableName());
                } else if (result.noRecords) {
                    tablesWithNoRecords.add(config.getTableName());
                }

                totalSuccessCount += result.successCount;
                totalFailureCount += result.failureCount;
                totalMsRecords += result.msRecordCount;
                totalSoiRecords += result.soiRecordCount;

                // Accumulate per-serviceId results
                ServiceResult sr = serviceResultMap.computeIfAbsent(
                        config.getServiceId(),
                        k -> ServiceResult.builder().serviceId(k).build());
                sr.setTotalRecordsExecuted(sr.getTotalRecordsExecuted() + result.successCount + result.failureCount);
                sr.setSuccessCount(sr.getSuccessCount() + result.successCount);
                sr.setFailureCount(sr.getFailureCount() + result.failureCount);
            }

            log.info("Comparison completed. Matched: {}, Unmatched: {}", totalSuccessCount, totalFailureCount);

            String status = determineOverallStatus(configs.size(), tablesWithNoRecords, tablesWithErrors);
            String message = buildResponseMessage(configs.size(), tablesWithNoRecords, tablesWithErrors);

            return ComparisonResponse.builder()
                    .status(status)
                    .message(message)
                    .totalRecordsMsDb(totalMsRecords)
                    .totalRecordsSoiDb(totalSoiRecords)
                    .totalComparedSuccessCount(totalSuccessCount)
                    .totalComparedFailureCount(totalFailureCount)
                    .configsProcessed(configs.size())
                    .serviceResults(new ArrayList<>(serviceResultMap.values()))
                    .build();

        } catch (Exception e) {
            log.error("Error during comparison", e);
            return ComparisonResponse.builder()
                    .status("ERROR")
                    .message("Comparison failed")
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Each config runs in its own transaction so failures are isolated.
     */
    private ComparisonResult processSingleConfig(ComparatorConfigEntity config) {
        TransactionTemplate txTemplate = new TransactionTemplate(db1TransactionManager);
        ComparisonResult result = new ComparisonResult();

        try {
            txTemplate.execute(status -> {
                processConfiguration(config, result);

                // Only update config status to 'Y' when records were found and compared
                if (!result.noRecords && !result.hasError) {
                    config.setExecutionStatus("Y");
                    config.setUpdatedAt(LocalDateTime.now());
                    configRepository.save(config);
                    log.info("Updated config ID: {} status to 'Y'", config.getId());
                }

                return null;
            });
        } catch (Exception e) {
            log.error("Transaction failed for config ID: {}: {}", config.getId(), e.getMessage());
            result.hasError = true;
        }

        return result;
    }

    private List<ComparatorConfigEntity> fetchActiveConfigs(ComparisonRequest request) {
        String serviceId = request.getServiceName();
        String tableName = request.getTableName();

        if (serviceId != null && serviceId.isBlank()) serviceId = null;
        if (tableName != null && tableName.isBlank()) tableName = null;

        log.debug("Fetching active configs - serviceId: {}, tableName: {}", serviceId, tableName);
        return configRepository.findActiveConfigsWithFilters(serviceId, tableName);
    }

    private void processConfiguration(ComparatorConfigEntity config, ComparisonResult result) {
        List<String> tableFields = Arrays.asList(config.getTableFields().split(","));
        List<String> primaryFields = Arrays.asList(config.getPrimaryFields().split(","));

        log.info("Table fields: {}, Primary keys: {}", tableFields, primaryFields);

        Stream<Map<String, Object>> msStreamOpened;
        Stream<Map<String, Object>> soiStreamOpened;
        boolean msError = false;
        boolean soiError = false;

        try {
            msStreamOpened = streamRecords(msEntityManager, config, tableFields, primaryFields, config.getMsServiceNameWhitelist());
        } catch (Exception e) {
            log.error("Error fetching MS records for table '{}': {}", config.getTableName(), e.getMessage());
            msStreamOpened = Stream.empty();
            msError = true;
        }

        try {
            soiStreamOpened = streamRecords(soiEntityManager, config, tableFields, primaryFields, config.getSoiServiceNameWhitelist());
        } catch (Exception e) {
            log.error("Error fetching SOI records for table '{}': {}", config.getTableName(), e.getMessage());
            soiStreamOpened = Stream.empty();
            soiError = true;
        }

        if (msError || soiError) {
            String errorDetail = msError && soiError ? "Error in both DBs"
                    : msError ? "Error in MS DB" : "Error in SOI DB";
            saveHistoryRecord(config, String.join(",", tableFields), "ERROR", errorDetail, errorDetail);
            result.hasError = true;
            msStreamOpened.close();
            soiStreamOpened.close();
            return;
        }

        final Stream<Map<String, Object>> msStream = msStreamOpened;
        final Stream<Map<String, Object>> soiStream = soiStreamOpened;

        try (msStream; soiStream) {
            Iterator<Map<String, Object>> msIter = msStream.iterator();
            Iterator<Map<String, Object>> soiIter = soiStream.iterator();

            Map<String, Object> msRow = advance(msIter);
            Map<String, Object> soiRow = advance(soiIter);

            // Both empty
            if (msRow == null && soiRow == null) {
                log.warn("No records found in table '{}' for the given date range", config.getTableName());
                saveHistoryRecord(config, String.join(",", tableFields), "NO_RECORD", "NO_RECORDS", "NO_RECORDS");
                result.noRecords = true;
                return;
            }
            // MS empty, SOI has records — count SOI records
            if (msRow == null) {
                log.warn("No records found in MS DB for table '{}'", config.getTableName());
                int soiCount = 1; // already have one row
                while (soiIter.hasNext()) { soiIter.next(); soiCount++; }
                result.soiRecordCount = soiCount;
                saveHistoryRecord(config, String.join(",", tableFields), "NO_RECORD",
                        "NO_RECORDS", soiCount + " records exist in SOI");
                result.noRecords = true;
                result.failureCount = soiCount;
                return;
            }
            // SOI empty, MS has records — count MS records
            if (soiRow == null) {
                log.warn("No records found in SOI DB for table '{}'", config.getTableName());
                int msCount = 1;
                while (msIter.hasNext()) { msIter.next(); msCount++; }
                result.msRecordCount = msCount;
                saveHistoryRecord(config, String.join(",", tableFields), "NO_RECORD",
                        msCount + " records exist in MS", "NO_RECORDS");
                result.noRecords = true;
                result.failureCount = msCount;
                return;
            }

            // Main merge loop
            List<ComparatorHistoryEntity> batch = new ArrayList<>(batchSize);

            while (msRow != null || soiRow != null) {
                String msKey = msRow != null ? buildKey(msRow, primaryFields) : null;
                String soiKey = soiRow != null ? buildKey(soiRow, primaryFields) : null;

                int cmp = compareKeys(msKey, soiKey);

                if (cmp == 0) {
                    // Keys equal — collect all duplicates with same key from both sides
                    List<Map<String, Object>> msDups = new ArrayList<>();
                    msDups.add(msRow);
                    result.msRecordCount++;
                    msRow = advance(msIter);
                    while (msRow != null && buildKey(msRow, primaryFields).equals(msKey)) {
                        msDups.add(msRow);
                        result.msRecordCount++;
                        msRow = advance(msIter);
                    }

                    List<Map<String, Object>> soiDups = new ArrayList<>();
                    soiDups.add(soiRow);
                    result.soiRecordCount++;
                    soiRow = advance(soiIter);
                    while (soiRow != null && buildKey(soiRow, primaryFields).equals(soiKey)) {
                        soiDups.add(soiRow);
                        result.soiRecordCount++;
                        soiRow = advance(soiIter);
                    }

                    int maxSize = Math.max(msDups.size(), soiDups.size());
                    for (int i = 0; i < maxSize; i++) {
                        Map<String, Object> ms = i < msDups.size() ? msDups.get(i) : null;
                        Map<String, Object> soi = i < soiDups.size() ? soiDups.get(i) : null;

                        String status;
                        List<String> differentFields;

                        if (ms == null) {
                            status = "MISSING_IN_MS";
                            differentFields = tableFields;
                        } else if (soi == null) {
                            status = "MISSING_IN_SOI";
                            differentFields = tableFields;
                        } else {
                            differentFields = findDifferentFields(ms, soi, tableFields);
                            status = differentFields.isEmpty() ? "MATCH" : "MISMATCH";
                        }

                        addToHistoryBatch(batch, config, differentFields, tableFields, status, ms, soi);

                        if ("MATCH".equals(status)) {
                            result.successCount++;
                        } else {
                            result.failureCount++;
                        }

                        if (batch.size() >= batchSize) {
                            flushBatch(batch);
                        }
                    }
                } else if (cmp < 0) {
                    // MS key < SOI key → MISSING_IN_SOI
                    addToHistoryBatch(batch, config, tableFields, tableFields, "MISSING_IN_SOI", msRow, null);
                    result.failureCount++;
                    result.msRecordCount++;
                    msRow = advance(msIter);

                    if (batch.size() >= batchSize) {
                        flushBatch(batch);
                    }
                } else {
                    // MS key > SOI key → MISSING_IN_MS
                    addToHistoryBatch(batch, config, tableFields, tableFields, "MISSING_IN_MS", null, soiRow);
                    result.failureCount++;
                    result.soiRecordCount++;
                    soiRow = advance(soiIter);

                    if (batch.size() >= batchSize) {
                        flushBatch(batch);
                    }
                }
            }

            // Flush remaining
            if (!batch.isEmpty()) {
                flushBatch(batch);
            }
        }
    }

    // ==================== Streaming ====================

    @SuppressWarnings("unchecked")
    private Stream<Map<String, Object>> streamRecords(
            EntityManager entityManager, ComparatorConfigEntity config,
            List<String> fields, List<String> primaryFields, String serviceNameWhitelist) {

        Set<String> allFields = new LinkedHashSet<>();
        primaryFields.forEach(f -> allFields.add(f.trim()));
        fields.forEach(f -> allFields.add(f.trim()));

        String fieldList = String.join(", ", allFields);
        String orderBy = primaryFields.stream()
                .map(String::trim)
                .collect(Collectors.joining(", "));
        String dateField = (config.getWhereDateField() != null && !config.getWhereDateField().isBlank())
                ? config.getWhereDateField().trim()
                : "LAST_UPDT_TS";

        String updatedByFilter = buildUpdatedByFilter(serviceNameWhitelist);
        // Use JDBC named parameters so each DB's driver handles date type conversion natively.
        // This avoids Oracle ORA-01861 errors caused by string-formatted date literals.
        String sql = String.format(
                "SELECT %s FROM %s WHERE %s >= :startDate AND %s <= :endDate%s ORDER BY %s",
                fieldList, config.getTableName(), dateField, dateField, updatedByFilter, orderBy);

        log.debug("Executing streaming query: {}", sql);

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("startDate", config.getStartDate());
        query.setParameter("endDate", config.getEndDate());
        query.setHint("org.hibernate.fetchSize", fetchSize);

        List<String> columnNames = new ArrayList<>(allFields);
        boolean singleColumn = columnNames.size() == 1;

        Stream<Object> rawStream = query.getResultStream();
        return rawStream.map(row -> {
            Map<String, Object> record = new HashMap<>();
            if (singleColumn) {
                record.put(columnNames.getFirst(), row);
            } else {
                Object[] cols = (Object[]) row;
                for (int i = 0; i < columnNames.size() && i < cols.length; i++) {
                    record.put(columnNames.get(i), cols[i]);
                }
            }
            return record;
        });
    }

    /**
     * Builds an AND UPDATED_BY IN (...) clause from the comma-separated whitelist.
     * Returns empty string when whitelist is null or blank.
     */
    private String buildUpdatedByFilter(String serviceNameWhitelist) {
        if (serviceNameWhitelist == null || serviceNameWhitelist.isBlank()) {
            return "";
        }
        String[] names = serviceNameWhitelist.split(",");
        String inClause = Arrays.stream(names)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(", "));
        return inClause.isEmpty() ? "" : " AND UPDATED_BY IN (" + inClause + ")";
    }

    // ==================== Merge Helpers ====================

    private Map<String, Object> advance(Iterator<Map<String, Object>> iter) {
        return iter.hasNext() ? iter.next() : null;
    }

    private String buildKey(Map<String, Object> record, List<String> primaryFields) {
        return primaryFields.stream()
                .map(pk -> String.valueOf(record.get(pk.trim())))
                .collect(Collectors.joining("||"));
    }

    private int compareKeys(String msKey, String soiKey) {
        if (msKey == null) return 1;   // null ms key → advance soi (ms exhausted)
        if (soiKey == null) return -1;  // null soi key → advance ms (soi exhausted)
        return msKey.compareTo(soiKey);
    }

    private void addToHistoryBatch(List<ComparatorHistoryEntity> batch,
                                   ComparatorConfigEntity config,
                                   List<String> differentFields, List<String> tableFields,
                                   String status, Map<String, Object> msRecord, Map<String, Object> soiRecord) {
        String msValues = msRecord != null ? convertToJson(msRecord) : "MISSING";
        String soiValues = soiRecord != null ? convertToJson(soiRecord) : "MISSING";
        String diffFields = differentFields.isEmpty()
                ? String.join(",", tableFields)
                : String.join(",", differentFields);

        batch.add(ComparatorHistoryEntity.builder()
                .serviceId(config.getServiceId())
                .tableName(config.getTableName())
                .fieldsName(diffFields)
                .comparatorExecutionStatus(status)
                .msTableValues(msValues)
                .soiTableValues(soiValues)
                .build());
    }

    private void flushBatch(List<ComparatorHistoryEntity> batch) {
        historyRepository.saveAll(batch);
        batch.clear();
    }

    // ==================== Comparison Helpers ====================

    private List<String> findDifferentFields(
            Map<String, Object> msRecord, Map<String, Object> soiRecord,
            List<String> fieldsToCompare) {
        List<String> differentFields = new ArrayList<>();
        for (String field : fieldsToCompare) {
            String fieldName = field.trim();
            if (!valuesAreEqual(msRecord.get(fieldName), soiRecord.get(fieldName))) {
                differentFields.add(fieldName);
            }
        }
        return differentFields;
    }

    private boolean valuesAreEqual(Object a, Object b) {
        if (Objects.equals(a, b)) return true;
        if (a == null || b == null) return false;

        // Temporal: truncate to seconds to absorb sub-second precision differences between DBs
        LocalDateTime dtA = toLocalDateTimeSeconds(a);
        LocalDateTime dtB = toLocalDateTimeSeconds(b);
        if (dtA != null || dtB != null) {
            // At least one side is temporal — only equal if both are temporal and match
            return dtA != null && dtA.equals(dtB);
        }

        // Numeric: compare by value to handle BigDecimal (Oracle) vs Long/Integer (MariaDB)
        if (a instanceof Number && b instanceof Number) {
            try {
                return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString())) == 0;
            } catch (NumberFormatException ignored) {
                // fall through to string comparison
            }
        }

        // String: trim trailing spaces to handle Oracle CHAR padding
        return a.toString().trim().equals(b.toString().trim());
    }

    private LocalDateTime toLocalDateTimeSeconds(Object value) {
        if (value instanceof LocalDateTime ldt) {
            return ldt.truncatedTo(ChronoUnit.SECONDS);
        }
        if (value instanceof Timestamp ts) {
            return ts.toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
        }
        if (value instanceof Date d) {
            return d.toLocalDate().atStartOfDay();
        }
        return null;
    }

    private String determineOverallStatus(int totalConfigs, List<String> noRecordTables, List<String> errorTables) {
        if (errorTables.size() == totalConfigs) return "ERROR";
        if (noRecordTables.size() + errorTables.size() == totalConfigs) return "WARNING";
        return "SUCCESS";
    }

    private String buildResponseMessage(int totalConfigs, List<String> noRecordTables, List<String> errorTables) {
        if (errorTables.size() == totalConfigs) {
            return "All configurations failed. Error in table(s): " + String.join(", ", errorTables);
        }

        StringBuilder message = new StringBuilder();

        if (noRecordTables.size() + errorTables.size() == totalConfigs) {
            if (!noRecordTables.isEmpty()) {
                message.append("No records in: ").append(String.join(", ", noRecordTables));
            }
            if (!errorTables.isEmpty()) {
                if (!message.isEmpty()) message.append(". ");
                message.append("Error in: ").append(String.join(", ", errorTables));
            }
            return message.toString();
        }

        message.append("Comparison completed successfully");
        if (!noRecordTables.isEmpty()) {
            message.append(". No records in: ").append(String.join(", ", noRecordTables));
        }
        if (!errorTables.isEmpty()) {
            message.append(". Error in: ").append(String.join(", ", errorTables));
        }
        return message.toString();
    }

    // ==================== History ====================

    private void saveHistoryRecord(ComparatorConfigEntity config, String fieldsName,
                                   String status, String msValues, String soiValues) {
        try {
            historyRepository.save(ComparatorHistoryEntity.builder()
                    .serviceId(config.getServiceId())
                    .tableName(config.getTableName())
                    .fieldsName(fieldsName)
                    .comparatorExecutionStatus(status)
                    .msTableValues(msValues)
                    .soiTableValues(soiValues)
                    .build());
            log.info("Saved history - service: {}, table: {}, status: {}",
                    config.getServiceId(), config.getTableName(), status);
        } catch (Exception e) {
            log.error("Error saving history for table '{}': {}", config.getTableName(), e.getMessage());
            throw e;
        }
    }

    private String convertToJson(Map<String, Object> record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            log.error("Error converting record to JSON", e);
            return record.toString();
        }
    }

    // ==================== Config Management ====================

    public List<ComparatorConfigEntity> getActiveConfigs() {
        return configRepository.findByExecutionStatus("N");
    }

    public List<ComparatorConfigEntity> getAllConfigs() {
        return configRepository.findAll();
    }

    @Transactional("db1TransactionManager")
    public ComparatorConfigEntity addConfig(ComparatorConfigEntity config) {
        log.info("Adding new config - serviceId: {}, tableName: {}", config.getServiceId(), config.getTableName());
        return configRepository.save(config);
    }

    @Transactional("db1TransactionManager")
    public ComparatorConfigEntity updateConfig(Long id, ComparatorConfigEntity updatedConfig) {
        log.info("Updating config ID: {}", id);
        ComparatorConfigEntity existing = configRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Config not found with id: " + id));

        existing.setServiceId(updatedConfig.getServiceId());
        existing.setTableName(updatedConfig.getTableName());
        existing.setTableFields(updatedConfig.getTableFields());
        existing.setPrimaryFields(updatedConfig.getPrimaryFields());
        existing.setExecutionStatus(updatedConfig.getExecutionStatus());
        existing.setMsServiceNameWhitelist(updatedConfig.getMsServiceNameWhitelist());
        existing.setSoiServiceNameWhitelist(updatedConfig.getSoiServiceNameWhitelist());
        existing.setWhereDateField(updatedConfig.getWhereDateField());
        existing.setStartDate(updatedConfig.getStartDate());
        existing.setEndDate(updatedConfig.getEndDate());

        return configRepository.save(existing);
    }

    // ==================== History Management ====================

    public List<ComparatorHistoryEntity> getComparisonHistory(String serviceId, String tableName) {
        if (serviceId != null && tableName != null) {
            return historyRepository.findByServiceIdAndTableName(serviceId, tableName);
        } else if (serviceId != null) {
            return historyRepository.findByServiceId(serviceId);
        } else if (tableName != null) {
            return historyRepository.findByTableName(tableName);
        }
        return historyRepository.findAll();
    }

    @Transactional("db1TransactionManager")
    public void clearHistory(String serviceId, String tableName) {
        List<ComparatorHistoryEntity> historyToDelete = getComparisonHistory(serviceId, tableName);
        historyRepository.deleteAll(historyToDelete);
        log.info("Deleted {} history records", historyToDelete.size());
    }

    // ==================== Inner Class ====================

    private static class ComparisonResult {
        int msRecordCount = 0;
        int soiRecordCount = 0;
        int successCount = 0;
        int failureCount = 0;
        boolean noRecords = false;
        boolean hasError = false;
    }
}