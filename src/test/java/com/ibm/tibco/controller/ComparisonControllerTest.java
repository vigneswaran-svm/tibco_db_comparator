package com.ibm.tibco.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.tibco.dto.ComparisonRequest;
import com.ibm.tibco.dto.ComparisonResponse;
import com.ibm.tibco.model.db1.ComparatorConfigEntity;
import com.ibm.tibco.model.db1.ComparatorHistoryEntity;
import com.ibm.tibco.service.ComparisonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ComparisonController.class)
class ComparisonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComparisonService comparisonService;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== POST /api/compare/execute ====================

    @Test
    void shouldExecuteComparison_withRequestBody() throws Exception {
        ComparisonResponse response = ComparisonResponse.builder()
                .status("SUCCESS")
                .message("Comparison completed successfully")
                .totalComparedSuccessCount(1)
                .configsProcessed(1)
                .build();

        when(comparisonService.compareTableRecords(any(ComparisonRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/compare/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceName\":\"SVC1\",\"tableName\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.totalComparedSuccessCount").value(1));
    }

    @Test
    void shouldExecuteComparison_withNullBody() throws Exception {
        ComparisonResponse response = ComparisonResponse.builder()
                .status("WARNING")
                .message("No active configurations found with status 'N'")
                .build();

        when(comparisonService.compareTableRecords(any())).thenReturn(response);

        mockMvc.perform(post("/api/compare/execute")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WARNING"));
    }

    @Test
    void shouldReturn500_whenExecuteThrowsException() throws Exception {
        when(comparisonService.compareTableRecords(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/compare/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"));
    }

    // ==================== GET /api/compare/configs/active ====================

    @Test
    void shouldGetActiveConfigs() throws Exception {
        when(comparisonService.getActiveConfigs()).thenReturn(List.of(
                ComparatorConfigEntity.builder().id(1L).serviceId("SVC1").tableName("T1")
                        .tableFields("F1").primaryFields("F1").executionStatus("N")
                        .startDate(LocalDateTime.now()).endDate(LocalDateTime.now()).build()
        ));

        mockMvc.perform(get("/api/compare/configs/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceId").value("SVC1"));
    }

    @Test
    void shouldReturn500_whenGetActiveConfigsFails() throws Exception {
        when(comparisonService.getActiveConfigs()).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/compare/configs/active"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== GET /api/compare/configs ====================

    @Test
    void shouldGetAllConfigs() throws Exception {
        when(comparisonService.getAllConfigs()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/compare/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldReturn500_whenGetAllConfigsFails() throws Exception {
        when(comparisonService.getAllConfigs()).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/compare/configs"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== POST /api/compare/configs ====================

    @Test
    void shouldAddConfig() throws Exception {
        ComparatorConfigEntity saved = ComparatorConfigEntity.builder()
                .id(1L).serviceId("SVC1").tableName("T1")
                .tableFields("F1").primaryFields("F1").executionStatus("N")
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now()).build();

        when(comparisonService.addConfig(any())).thenReturn(saved);

        mockMvc.perform(post("/api/compare/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":\"SVC1\",\"tableName\":\"T1\",\"tableFields\":\"F1\"," +
                                "\"primaryFields\":\"F1\",\"executionStatus\":\"N\"," +
                                "\"startDate\":\"2026-02-01T00:00:00\",\"endDate\":\"2026-02-10T23:59:59\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceId").value("SVC1"));
    }

    @Test
    void shouldReturn500_whenAddConfigFails() throws Exception {
        when(comparisonService.addConfig(any())).thenThrow(new RuntimeException("Save failed"));

        mockMvc.perform(post("/api/compare/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":\"SVC1\",\"tableName\":\"T1\",\"tableFields\":\"F1\"," +
                                "\"primaryFields\":\"F1\",\"executionStatus\":\"N\"," +
                                "\"startDate\":\"2026-02-01T00:00:00\",\"endDate\":\"2026-02-10T23:59:59\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Save failed"));
    }

    // ==================== PUT /api/compare/configs/update ====================

    @Test
    void shouldUpdateConfig() throws Exception {
        ComparatorConfigEntity updated = ComparatorConfigEntity.builder()
                .id(1L).serviceId("UPDATED").tableName("T1")
                .tableFields("F1").primaryFields("F1").executionStatus("N")
                .startDate(LocalDateTime.now()).endDate(LocalDateTime.now()).build();

        when(comparisonService.updateConfig(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/compare/configs/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"serviceId\":\"UPDATED\",\"tableName\":\"T1\",\"tableFields\":\"F1\"," +
                                "\"primaryFields\":\"F1\",\"executionStatus\":\"N\"," +
                                "\"startDate\":\"2026-02-01T00:00:00\",\"endDate\":\"2026-02-10T23:59:59\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value("UPDATED"));
    }

    @Test
    void shouldReturn400_whenUpdateWithoutId() throws Exception {
        mockMvc.perform(put("/api/compare/configs/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceId\":\"SVC1\",\"tableName\":\"T1\",\"tableFields\":\"F1\"," +
                                "\"primaryFields\":\"F1\",\"executionStatus\":\"N\"," +
                                "\"startDate\":\"2026-02-01T00:00:00\",\"endDate\":\"2026-02-10T23:59:59\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Config id is required"));
    }

    @Test
    void shouldReturn404_whenUpdateConfigNotFound() throws Exception {
        when(comparisonService.updateConfig(eq(999L), any()))
                .thenThrow(new RuntimeException("Config not found with id: 999"));

        mockMvc.perform(put("/api/compare/configs/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":999,\"serviceId\":\"SVC1\",\"tableName\":\"T1\",\"tableFields\":\"F1\"," +
                                "\"primaryFields\":\"F1\",\"executionStatus\":\"N\"," +
                                "\"startDate\":\"2026-02-01T00:00:00\",\"endDate\":\"2026-02-10T23:59:59\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Config not found with id: 999"));
    }

    // ==================== GET /api/compare/history ====================

    @Test
    void shouldGetHistory_withNoFilters() throws Exception {
        when(comparisonService.getComparisonHistory(null, null)).thenReturn(List.of(
                ComparatorHistoryEntity.builder().id(1L).serviceId("SVC1").tableName("T1")
                        .fieldsName("F1").comparatorExecutionStatus("MATCH")
                        .msTableValues("{}").soiTableValues("{}").build()
        ));

        mockMvc.perform(get("/api/compare/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].comparatorExecutionStatus").value("MATCH"));
    }

    @Test
    void shouldGetHistory_withServiceIdFilter() throws Exception {
        when(comparisonService.getComparisonHistory("SVC1", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/compare/history").param("serviceId", "SVC1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetHistory_withBothFilters() throws Exception {
        when(comparisonService.getComparisonHistory("SVC1", "T1")).thenReturn(List.of());

        mockMvc.perform(get("/api/compare/history")
                        .param("serviceId", "SVC1")
                        .param("tableName", "T1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn500_whenGetHistoryFails() throws Exception {
        when(comparisonService.getComparisonHistory(any(), any()))
                .thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(get("/api/compare/history"))
                .andExpect(status().isInternalServerError());
    }

    // ==================== DELETE /api/compare/history ====================

    @Test
    void shouldClearHistory() throws Exception {
        doNothing().when(comparisonService).clearHistory(null, null);

        mockMvc.perform(delete("/api/compare/history"))
                .andExpect(status().isOk())
                .andExpect(content().string("Comparison history cleared successfully"));
    }

    @Test
    void shouldClearHistory_withFilters() throws Exception {
        doNothing().when(comparisonService).clearHistory("SVC1", "T1");

        mockMvc.perform(delete("/api/compare/history")
                        .param("serviceId", "SVC1")
                        .param("tableName", "T1"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn500_whenClearHistoryFails() throws Exception {
        doThrow(new RuntimeException("Delete failed")).when(comparisonService).clearHistory(any(), any());

        mockMvc.perform(delete("/api/compare/history"))
                .andExpect(status().isInternalServerError());
    }
}