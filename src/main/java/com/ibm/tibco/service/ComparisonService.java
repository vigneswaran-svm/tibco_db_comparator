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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

        // Fetch records — errors are distinguished from empty results
        List<Map<String, Object>> msRecords;
        List<Map<String, Object>> soiRecords;
        boolean msError = false;
        boolean soiError = false;

        try {
            msRecords = fetchRecords(msEntityManager, config, tableFields, primaryFields);
        } catch (Exception e) {
            log.error("Error fetching MS records for table '{}': {}", config.getTableName(), e.getMessage());
            msRecords = Collections.emptyList();
            msError = true;
        }

        try {
            soiRecords = fetchRecords(soiEntityManager, config, tableFields, primaryFields);
        } catch (Exception e) {
            log.error("Error fetching SOI records for table '{}': {}", config.getTableName(), e.getMessage());
            soiRecords = Collections.emptyList();
            soiError = true;
        }

        result.msRecordCount = msRecords.size();
        result.soiRecordCount = soiRecords.size();

        log.info("Fetched {} MS records, {} SOI records (msError={}, soiError={})",
                msRecords.size(), soiRecords.size(), msError, soiError);

        // Handle fetch errors — save error history and return
        if (msError || soiError) {
            String errorDetail = msError && soiError ? "Error in both DBs"
                    : msError ? "Error in MS DB" : "Error in SOI DB";
            saveHistoryRecord(config, String.join(",", tableFields), "ERROR", errorDetail, errorDetail);
            result.hasError = true;
            return;
        }

        // Handle empty results — save no-record history
        if (msRecords.isEmpty() && soiRecords.isEmpty()) {
            log.warn("No records found in table '{}' for the given date range", config.getTableName());
            saveHistoryRecord(config, String.join(",", tableFields), "NO_RECORD", "NO_RECORDS", "NO_RECORDS");
            result.noRecords = true;
            return;
        }
        if (msRecords.isEmpty()) {
            log.warn("No records found in MS DB for table '{}'", config.getTableName());
            saveHistoryRecord(config, String.join(",", tableFields), "NO_RECORD",
                    "NO_RECORDS", soiRecords.size() + " records exist in SOI");
            result.noRecords = true;
            result.failureCount = soiRecords.size();
            return;
        }
        if (soiRecords.isEmpty()) {
            log.warn("No records found in SOI DB for table '{}'", config.getTableName());
            saveHistoryRecord(config, String.join(",", tableFields), "NO_RECORD",
                    msRecords.size() + " records exist in MS", "NO_RECORDS");
            result.noRecords = true;
            result.failureCount = msRecords.size();
            return;
        }

        // Group records by primary key (supports multiple records per key)
        Map<String, List<Map<String, Object>>> msMap = groupRecordsByKey(msRecords, primaryFields);
        Map<String, List<Map<String, Object>>> soiMap = groupRecordsByKey(soiRecords, primaryFields);

        Set<String> allKeys = new LinkedHashSet<>();
        allKeys.addAll(msMap.keySet());
        allKeys.addAll(soiMap.keySet());

        for (String key : allKeys) {
            List<Map<String, Object>> msList = msMap.getOrDefault(key, Collections.emptyList());
            List<Map<String, Object>> soiList = soiMap.getOrDefault(key, Collections.emptyList());

            int maxSize = Math.max(msList.size(), soiList.size());

            for (int i = 0; i < maxSize; i++) {
                Map<String, Object> msRecord = i < msList.size() ? msList.get(i) : null;
                Map<String, Object> soiRecord = i < soiList.size() ? soiList.get(i) : null;

                String status;
                List<String> differentFields;

                if (msRecord == null) {
                    status = "MISSING_IN_MS";
                    differentFields = tableFields;
                } else if (soiRecord == null) {
                    status = "MISSING_IN_SOI";
                    differentFields = tableFields;
                } else {
                    differentFields = findDifferentFields(msRecord, soiRecord, tableFields);
                    status = differentFields.isEmpty() ? "MATCH" : "MISMATCH";
                }

                String msValues = msRecord != null ? convertToJson(msRecord) : "MISSING";
                String soiValues = soiRecord != null ? convertToJson(soiRecord) : "MISSING";
                String diffFields = differentFields.isEmpty()
                        ? String.join(",", tableFields)
                        : String.join(",", differentFields);

                saveHistoryRecord(config, diffFields, status, msValues, soiValues);

                if ("MATCH".equals(status)) {
                    result.successCount++;
                } else {
                    result.failureCount++;
                }
            }
        }
    }

    // ==================== Record Fetching ====================

    /**
     * Fetches records from the given EntityManager. Throws exceptions on SQL errors
     * so the caller can distinguish errors from empty results.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchRecords(
            EntityManager entityManager, ComparatorConfigEntity config,
            List<String> fields, List<String> primaryFields) {

        Set<String> allFields = new LinkedHashSet<>();
        primaryFields.forEach(f -> allFields.add(f.trim()));
        fields.forEach(f -> allFields.add(f.trim()));

        String fieldList = String.join(", ", allFields);
        String startDateStr = config.getStartDate().format(DATE_FORMATTER);
        String endDateStr = config.getEndDate().format(DATE_FORMATTER);

        String sql = String.format(
                "SELECT %s FROM %s WHERE LAST_UPDT_TS >= '%s' AND LAST_UPDT_TS <= '%s'",
                fieldList, config.getTableName(), startDateStr, endDateStr);

        log.debug("Executing query: {}", sql);

        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> results = query.getResultList();

        List<String> columnNames = new ArrayList<>(allFields);
        List<Map<String, Object>> records = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> record = new HashMap<>();
            for (int i = 0; i < columnNames.size() && i < row.length; i++) {
                record.put(columnNames.get(i), row[i]);
            }
            records.add(record);
        }
        return records;
    }

    // ==================== Comparison Helpers ====================

    private Map<String, List<Map<String, Object>>> groupRecordsByKey(
            List<Map<String, Object>> records, List<String> primaryFields) {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        for (Map<String, Object> record : records) {
            String key = primaryFields.stream()
                    .map(primaryKey -> String.valueOf(record.get(primaryKey.trim())))
                    .collect(Collectors.joining("||"));
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        }
        return map;
    }

    private List<String> findDifferentFields(
            Map<String, Object> msRecord, Map<String, Object> soiRecord,
            List<String> fieldsToCompare) {
        List<String> differentFields = new ArrayList<>();
        for (String field : fieldsToCompare) {
            String fieldName = field.trim();
            if (!Objects.equals(msRecord.get(fieldName), soiRecord.get(fieldName))) {
                differentFields.add(fieldName);
            }
        }
        return differentFields;
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