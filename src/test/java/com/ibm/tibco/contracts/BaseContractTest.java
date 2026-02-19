package com.ibm.tibco.contracts;

import com.ibm.tibco.controller.ComparisonController;
import com.ibm.tibco.dto.ComparisonRequest;
import com.ibm.tibco.dto.ComparisonResponse;
import com.ibm.tibco.dto.ServiceResult;
import com.ibm.tibco.model.db1.ComparatorConfigEntity;
import com.ibm.tibco.model.db1.ComparatorHistoryEntity;
import com.ibm.tibco.service.ComparisonService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class BaseContractTest {

    @Autowired
    private ComparisonController comparisonController;

    @MockBean
    private ComparisonService comparisonService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.standaloneSetup(comparisonController);
        setupMocks();
    }

    private void setupMocks() {
        // Mock: execute comparison
        ServiceResult serviceResult = ServiceResult.builder()
                .serviceId("CORRESPONDENCEREQUEST")
                .totalRecordsExecuted(2)
                .successCount(1)
                .failureCount(1)
                .build();

        ComparisonResponse comparisonResponse = ComparisonResponse.builder()
                .status("SUCCESS")
                .message("Comparison completed successfully")
                .totalRecordsMsDb(2)
                .totalRecordsSoiDb(2)
                .totalComparedSuccessCount(1)
                .totalComparedFailureCount(1)
                .configsProcessed(1)
                .serviceResults(List.of(serviceResult))
                .build();

        Mockito.when(comparisonService.compareTableRecords(any(ComparisonRequest.class)))
                .thenReturn(comparisonResponse);

        // Mock: get all configs
        List<ComparatorConfigEntity> allConfigs = List.of(
                buildConfig(1L, "CORRESPONDENCEREQUEST", "NOTIFCTN_LOG", "N"),
                buildConfig(2L, "PROFILEENQUIRY", "PROFILE_ENQUIRY", "Y")
        );
        Mockito.when(comparisonService.getAllConfigs()).thenReturn(allConfigs);

        // Mock: get active configs
        List<ComparatorConfigEntity> activeConfigs = List.of(
                buildConfig(1L, "CORRESPONDENCEREQUEST", "NOTIFCTN_LOG", "N")
        );
        Mockito.when(comparisonService.getActiveConfigs()).thenReturn(activeConfigs);

        // Mock: add config
        ComparatorConfigEntity savedConfig = buildConfig(3L, "TESTSERVICE", "TEST_TABLE", "N");
        savedConfig.setTableFields("FIELD1,FIELD2,FIELD3");
        savedConfig.setPrimaryFields("FIELD1");
        Mockito.when(comparisonService.addConfig(any(ComparatorConfigEntity.class)))
                .thenReturn(savedConfig);

        // Mock: update config
        ComparatorConfigEntity updatedConfig = buildConfig(1L, "UPDATEDSERVICE", "UPDATED_TABLE", "N");
        Mockito.when(comparisonService.updateConfig(anyLong(), any(ComparatorConfigEntity.class)))
                .thenReturn(updatedConfig);

        // Mock: get history
        List<ComparatorHistoryEntity> history = List.of(
                buildHistory(1L, "CORRESPONDENCEREQUEST", "NOTIFCTN_LOG", "MATCH"),
                buildHistory(2L, "CORRESPONDENCEREQUEST", "NOTIFCTN_LOG", "MISMATCH")
        );
        Mockito.when(comparisonService.getComparisonHistory(any(), any())).thenReturn(history);
        Mockito.when(comparisonService.getComparisonHistory(anyString(), any())).thenReturn(history);

        // Mock: clear history
        Mockito.doNothing().when(comparisonService).clearHistory(any(), any());
    }

    private ComparatorConfigEntity buildConfig(Long id, String serviceId, String tableName, String status) {
        return ComparatorConfigEntity.builder()
                .id(id)
                .serviceId(serviceId)
                .tableName(tableName)
                .tableFields("FIELD1,FIELD2")
                .primaryFields("FIELD1")
                .executionStatus(status)
                .startDate(LocalDateTime.of(2026, 2, 1, 0, 0))
                .endDate(LocalDateTime.of(2026, 2, 10, 23, 59, 59))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private ComparatorHistoryEntity buildHistory(Long id, String serviceId, String tableName, String status) {
        return ComparatorHistoryEntity.builder()
                .id(id)
                .serviceId(serviceId)
                .tableName(tableName)
                .fieldsName("FIELD1,FIELD2")
                .comparatorExecutionStatus(status)
                .msTableValues("{\"FIELD1\":\"val1\"}")
                .soiTableValues("{\"FIELD1\":\"val1\"}")
                .createdAt(LocalDateTime.now())
                .build();
    }
}