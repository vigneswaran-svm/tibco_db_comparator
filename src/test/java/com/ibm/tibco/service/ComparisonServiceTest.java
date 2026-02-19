package com.ibm.tibco.service;

import com.ibm.tibco.dto.ComparisonRequest;
import com.ibm.tibco.dto.ComparisonResponse;
import com.ibm.tibco.model.db1.ComparatorConfigEntity;
import com.ibm.tibco.model.db1.ComparatorHistoryEntity;
import com.ibm.tibco.repository.ComparatorConfigRepository;
import com.ibm.tibco.repository.ComparatorHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComparisonServiceTest {

    @InjectMocks
    private ComparisonService comparisonService;

    @Mock
    private ComparatorConfigRepository configRepository;

    @Mock
    private ComparatorHistoryRepository historyRepository;

    @Mock
    private EntityManager msEntityManager;

    @Mock
    private EntityManager soiEntityManager;

    @Mock
    private PlatformTransactionManager db1TransactionManager;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(comparisonService, "msEntityManager", msEntityManager);
        ReflectionTestUtils.setField(comparisonService, "soiEntityManager", soiEntityManager);
        ReflectionTestUtils.setField(comparisonService, "db1TransactionManager", db1TransactionManager);

        // Mock TransactionTemplate behavior — execute the callback directly
        lenient().doAnswer(invocation -> new SimpleTransactionStatus())
                .when(db1TransactionManager).getTransaction(any());

        lenient().doNothing().when(db1TransactionManager).commit(any());
        lenient().doNothing().when(db1TransactionManager).rollback(any());
    }

    // ==================== Helper Methods ====================

    private ComparatorConfigEntity buildConfig(Long id, String serviceId, String tableName) {
        return ComparatorConfigEntity.builder()
                .id(id)
                .serviceId(serviceId)
                .tableName(tableName)
                .tableFields("FIELD1,FIELD2,FIELD3")
                .primaryFields("FIELD1")
                .executionStatus("N")
                .startDate(LocalDateTime.of(2026, 2, 1, 0, 0))
                .endDate(LocalDateTime.of(2026, 2, 10, 23, 59, 59))
                .build();
    }

    private Query mockQueryWithResults(EntityManager em, List<Object[]> results) {
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(results);
        return query;
    }

    private void mockQueryWithException(EntityManager em, RuntimeException ex) {
        Query query = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenThrow(ex);
    }

    /** Helper to create a single-element list of Object[] without varargs ambiguity on JDK 25 */
    private List<Object[]> singleRow(Object... values) {
        return Collections.singletonList(values);
    }

    /** Helper to create a two-element list of Object[] */
    private List<Object[]> twoRows(Object[] row1, Object[] row2) {
        List<Object[]> list = new ArrayList<>();
        list.add(row1);
        list.add(row2);
        return list;
    }

    // ==================== compareTableRecords ====================

    @Nested
    class CompareTableRecordsTests {

        @Test
        void shouldReturnWarning_whenNoActiveConfigs() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(Collections.emptyList());

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("WARNING", response.getStatus());
            assertEquals("No active configurations found with status 'N'", response.getMessage());
        }

        @Test
        void shouldReturnSuccess_whenRecordsMatch() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithResults(msEntityManager, singleRow("KEY1", "VAL1", "VAL2"));
            mockQueryWithResults(soiEntityManager, singleRow("KEY1", "VAL1", "VAL2"));

            when(historyRepository.save(any(ComparatorHistoryEntity.class)))
                    .thenReturn(ComparatorHistoryEntity.builder().id(1L).build());
            when(configRepository.save(any(ComparatorConfigEntity.class))).thenReturn(config);

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals(1, response.getTotalComparedSuccessCount());
            assertEquals(0, response.getTotalComparedFailureCount());
            assertEquals(1, response.getConfigsProcessed());
            assertNotNull(response.getServiceResults());
            assertEquals(1, response.getServiceResults().size());
            assertEquals("SVC1", response.getServiceResults().get(0).getServiceId());
        }

        @Test
        void shouldReturnMismatch_whenRecordsDiffer() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithResults(msEntityManager, singleRow("KEY1", "VAL1", "VAL2"));
            mockQueryWithResults(soiEntityManager, singleRow("KEY1", "DIFFERENT", "VAL2"));

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());
            when(configRepository.save(any())).thenReturn(config);

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals(0, response.getTotalComparedSuccessCount());
            assertEquals(1, response.getTotalComparedFailureCount());
        }

        @Test
        void shouldHandleMissingInMs_whenKeyOnlyInSoi() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithResults(msEntityManager, singleRow("KEY1", "V1", "V2"));
            mockQueryWithResults(soiEntityManager, twoRows(
                    new Object[]{"KEY1", "V1", "V2"},
                    new Object[]{"KEY2", "V3", "V4"}
            ));

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());
            when(configRepository.save(any())).thenReturn(config);

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals(1, response.getTotalComparedSuccessCount());
            assertEquals(1, response.getTotalComparedFailureCount()); // MISSING_IN_MS
        }

        @Test
        void shouldHandleMissingInSoi_whenKeyOnlyInMs() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithResults(msEntityManager, twoRows(
                    new Object[]{"KEY1", "V1", "V2"},
                    new Object[]{"KEY2", "V3", "V4"}
            ));
            mockQueryWithResults(soiEntityManager, singleRow("KEY1", "V1", "V2"));

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());
            when(configRepository.save(any())).thenReturn(config);

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals(1, response.getTotalComparedSuccessCount());
            assertEquals(1, response.getTotalComparedFailureCount()); // MISSING_IN_SOI
        }

        @Test
        void shouldReturnWarning_whenBothDbsHaveNoRecords() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithResults(msEntityManager, Collections.emptyList());
            mockQueryWithResults(soiEntityManager, Collections.emptyList());

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("WARNING", response.getStatus());
            assertTrue(response.getMessage().contains("No records in"));
        }

        @Test
        void shouldHandleNoRecordsInMsOnly() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithResults(msEntityManager, Collections.emptyList());
            mockQueryWithResults(soiEntityManager, singleRow("K1", "V1", "V2"));

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("WARNING", response.getStatus());
        }

        @Test
        void shouldHandleNoRecordsInSoiOnly() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithResults(msEntityManager, singleRow("K1", "V1", "V2"));
            mockQueryWithResults(soiEntityManager, Collections.emptyList());

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("WARNING", response.getStatus());
        }

        @Test
        void shouldHandleSqlErrorInMsDb() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithException(msEntityManager, new RuntimeException("Unknown column"));
            mockQueryWithResults(soiEntityManager, singleRow("K1", "V1", "V2"));

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Error"));
        }

        @Test
        void shouldHandleSqlErrorInSoiDb() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithResults(msEntityManager, singleRow("K1", "V1", "V2"));
            mockQueryWithException(soiEntityManager, new RuntimeException("Table not found"));

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("ERROR", response.getStatus());
        }

        @Test
        void shouldHandleSqlErrorInBothDbs() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            mockQueryWithException(msEntityManager, new RuntimeException("Error1"));
            mockQueryWithException(soiEntityManager, new RuntimeException("Error2"));

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("ERROR", response.getStatus());
        }

        @Test
        void shouldHandleMultipleConfigs_withMixedResults() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config1 = buildConfig(1L, "SVC1", "TABLE1");
            ComparatorConfigEntity config2 = buildConfig(2L, "SVC2", "TABLE2");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config1, config2));

            // Config1: success, Config2: no records
            Query q1 = mock(Query.class);
            Query q2 = mock(Query.class);
            Query q3 = mock(Query.class);
            Query q4 = mock(Query.class);

            when(msEntityManager.createNativeQuery(contains("TABLE1"))).thenReturn(q1);
            when(soiEntityManager.createNativeQuery(contains("TABLE1"))).thenReturn(q2);
            when(msEntityManager.createNativeQuery(contains("TABLE2"))).thenReturn(q3);
            when(soiEntityManager.createNativeQuery(contains("TABLE2"))).thenReturn(q4);

            when(q1.getResultList()).thenReturn(singleRow("K1", "V1", "V2"));
            when(q2.getResultList()).thenReturn(singleRow("K1", "V1", "V2"));
            when(q3.getResultList()).thenReturn(Collections.emptyList());
            when(q4.getResultList()).thenReturn(Collections.emptyList());

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());
            when(configRepository.save(any())).thenReturn(config1);

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("SUCCESS", response.getStatus());
            assertTrue(response.getMessage().contains("No records in"));
            assertEquals(2, response.getConfigsProcessed());
        }

        @Test
        void shouldHandleMultipleRecordsWithSamePrimaryKey() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            // 2 records with same primary key, different values
            List<Object[]> msResults = twoRows(
                    new Object[]{"KEY1", "VAL_A", "VAL_B"},
                    new Object[]{"KEY1", "VAL_C", "VAL_D"}
            );
            List<Object[]> soiResults = twoRows(
                    new Object[]{"KEY1", "VAL_A", "VAL_B"},
                    new Object[]{"KEY1", "VAL_X", "VAL_Y"}
            );

            mockQueryWithResults(msEntityManager, msResults);
            mockQueryWithResults(soiEntityManager, soiResults);

            when(historyRepository.save(any())).thenReturn(ComparatorHistoryEntity.builder().id(1L).build());
            when(configRepository.save(any())).thenReturn(config);

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("SUCCESS", response.getStatus());
            assertEquals(1, response.getTotalComparedSuccessCount());  // first pair matches
            assertEquals(1, response.getTotalComparedFailureCount());  // second pair mismatches
            assertEquals(2, response.getTotalRecordsMsDb());
            assertEquals(2, response.getTotalRecordsSoiDb());
        }

        @Test
        void shouldReturnError_whenUnexpectedException() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            when(configRepository.findActiveConfigsWithFilters(any(), any()))
                    .thenThrow(new RuntimeException("DB connection failed"));

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("ERROR", response.getStatus());
            assertEquals("Comparison failed", response.getMessage());
            assertEquals("DB connection failed", response.getError());
        }

        @Test
        void shouldConvertBlankServiceNameToNull() {
            ComparisonRequest request = ComparisonRequest.builder()
                    .serviceName("  ")
                    .tableName("")
                    .build();

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(Collections.emptyList());

            comparisonService.compareTableRecords(request);

            verify(configRepository).findActiveConfigsWithFilters(null, null);
        }

        @Test
        void shouldPassNonBlankServiceNameAsIs() {
            ComparisonRequest request = ComparisonRequest.builder()
                    .serviceName("SVC1")
                    .tableName("TABLE1")
                    .build();

            when(configRepository.findActiveConfigsWithFilters("SVC1", "TABLE1"))
                    .thenReturn(Collections.emptyList());

            comparisonService.compareTableRecords(request);

            verify(configRepository).findActiveConfigsWithFilters("SVC1", "TABLE1");
        }

        @Test
        void shouldHandleTransactionFailure() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            // Make transaction manager throw on getTransaction
            when(db1TransactionManager.getTransaction(any()))
                    .thenThrow(new RuntimeException("Transaction error"));

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("ERROR", response.getStatus());
        }

        @Test
        void shouldReturnAllErrorStatus_whenAllConfigsFail() {
            ComparisonRequest request = ComparisonRequest.builder().build();
            ComparatorConfigEntity config = buildConfig(1L, "SVC1", "TABLE1");

            when(configRepository.findActiveConfigsWithFilters(null, null))
                    .thenReturn(List.of(config));

            when(db1TransactionManager.getTransaction(any()))
                    .thenThrow(new RuntimeException("TX error"));

            ComparisonResponse response = comparisonService.compareTableRecords(request);

            assertEquals("ERROR", response.getStatus());
            assertTrue(response.getMessage().contains("Error"));
        }
    }

    // ==================== Config Management ====================

    @Nested
    class ConfigManagementTests {

        @Test
        void shouldGetActiveConfigs() {
            List<ComparatorConfigEntity> configs = List.of(buildConfig(1L, "SVC1", "T1"));
            when(configRepository.findByExecutionStatus("N")).thenReturn(configs);

            List<ComparatorConfigEntity> result = comparisonService.getActiveConfigs();

            assertEquals(1, result.size());
            verify(configRepository).findByExecutionStatus("N");
        }

        @Test
        void shouldGetAllConfigs() {
            List<ComparatorConfigEntity> configs = List.of(
                    buildConfig(1L, "SVC1", "T1"),
                    buildConfig(2L, "SVC2", "T2")
            );
            when(configRepository.findAll()).thenReturn(configs);

            List<ComparatorConfigEntity> result = comparisonService.getAllConfigs();

            assertEquals(2, result.size());
            verify(configRepository).findAll();
        }

        @Test
        void shouldAddConfig() {
            ComparatorConfigEntity config = buildConfig(null, "NEW_SVC", "NEW_TABLE");
            ComparatorConfigEntity saved = buildConfig(3L, "NEW_SVC", "NEW_TABLE");
            when(configRepository.save(config)).thenReturn(saved);

            ComparatorConfigEntity result = comparisonService.addConfig(config);

            assertEquals(3L, result.getId());
            verify(configRepository).save(config);
        }

        @Test
        void shouldUpdateConfig_whenExists() {
            ComparatorConfigEntity existing = buildConfig(1L, "OLD_SVC", "OLD_TABLE");
            ComparatorConfigEntity updated = buildConfig(1L, "NEW_SVC", "NEW_TABLE");

            when(configRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(configRepository.save(any())).thenReturn(updated);

            ComparatorConfigEntity result = comparisonService.updateConfig(1L, updated);

            assertEquals("NEW_SVC", result.getServiceId());
            verify(configRepository).findById(1L);
            verify(configRepository).save(existing);
        }

        @Test
        void shouldThrowException_whenUpdateConfigNotFound() {
            when(configRepository.findById(999L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> comparisonService.updateConfig(999L, buildConfig(999L, "X", "Y")));

            assertEquals("Config not found with id: 999", ex.getMessage());
        }
    }

    // ==================== History Management ====================

    @Nested
    class HistoryManagementTests {

        @Test
        void shouldGetHistory_whenBothServiceIdAndTableName() {
            List<ComparatorHistoryEntity> history = List.of(
                    ComparatorHistoryEntity.builder().id(1L).build()
            );
            when(historyRepository.findByServiceIdAndTableName("SVC1", "T1")).thenReturn(history);

            List<ComparatorHistoryEntity> result = comparisonService.getComparisonHistory("SVC1", "T1");

            assertEquals(1, result.size());
            verify(historyRepository).findByServiceIdAndTableName("SVC1", "T1");
        }

        @Test
        void shouldGetHistory_whenOnlyServiceId() {
            when(historyRepository.findByServiceId("SVC1")).thenReturn(List.of());

            comparisonService.getComparisonHistory("SVC1", null);

            verify(historyRepository).findByServiceId("SVC1");
        }

        @Test
        void shouldGetHistory_whenOnlyTableName() {
            when(historyRepository.findByTableName("T1")).thenReturn(List.of());

            comparisonService.getComparisonHistory(null, "T1");

            verify(historyRepository).findByTableName("T1");
        }

        @Test
        void shouldGetAllHistory_whenNoFilters() {
            when(historyRepository.findAll()).thenReturn(List.of());

            comparisonService.getComparisonHistory(null, null);

            verify(historyRepository).findAll();
        }

        @Test
        void shouldClearHistory() {
            List<ComparatorHistoryEntity> history = List.of(
                    ComparatorHistoryEntity.builder().id(1L).build(),
                    ComparatorHistoryEntity.builder().id(2L).build()
            );
            when(historyRepository.findAll()).thenReturn(history);

            comparisonService.clearHistory(null, null);

            verify(historyRepository).deleteAll(history);
        }

        @Test
        void shouldClearHistoryByServiceId() {
            List<ComparatorHistoryEntity> history = List.of(
                    ComparatorHistoryEntity.builder().id(1L).build()
            );
            when(historyRepository.findByServiceId("SVC1")).thenReturn(history);

            comparisonService.clearHistory("SVC1", null);

            verify(historyRepository).deleteAll(history);
        }
    }
}