package com.ibm.tibco.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonResponseTest {

    @Test
    void shouldBuildWithAllFields() {
        ServiceResult sr = ServiceResult.builder()
                .serviceId("SVC1").totalRecordsExecuted(5)
                .successCount(3).failureCount(2).build();

        ComparisonResponse response = ComparisonResponse.builder()
                .status("SUCCESS")
                .message("Comparison completed successfully")
                .totalRecordsMsDb(10)
                .totalRecordsSoiDb(10)
                .totalComparedSuccessCount(8)
                .totalComparedFailureCount(2)
                .configsProcessed(3)
                .serviceResults(List.of(sr))
                .error(null)
                .build();

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Comparison completed successfully", response.getMessage());
        assertEquals(10, response.getTotalRecordsMsDb());
        assertEquals(10, response.getTotalRecordsSoiDb());
        assertEquals(8, response.getTotalComparedSuccessCount());
        assertEquals(2, response.getTotalComparedFailureCount());
        assertEquals(3, response.getConfigsProcessed());
        assertNotNull(response.getServiceResults());
        assertEquals(1, response.getServiceResults().size());
        assertNull(response.getError());
    }

    @Test
    void shouldBuildWithMinimalFields() {
        ComparisonResponse response = ComparisonResponse.builder()
                .status("WARNING")
                .message("No active configurations found")
                .build();

        assertEquals("WARNING", response.getStatus());
        assertEquals("No active configurations found", response.getMessage());
        assertEquals(0, response.getTotalRecordsMsDb());
        assertEquals(0, response.getTotalRecordsSoiDb());
        assertEquals(0, response.getTotalComparedSuccessCount());
        assertEquals(0, response.getTotalComparedFailureCount());
        assertEquals(0, response.getConfigsProcessed());
        assertNull(response.getServiceResults());
        assertNull(response.getError());
    }

    @Test
    void shouldBuildWithErrorField() {
        ComparisonResponse response = ComparisonResponse.builder()
                .status("ERROR")
                .message("Comparison failed")
                .error("Connection refused")
                .build();

        assertEquals("ERROR", response.getStatus());
        assertEquals("Connection refused", response.getError());
    }

    @Test
    void shouldCreateWithNoArgsConstructor() {
        ComparisonResponse response = new ComparisonResponse();

        assertNull(response.getStatus());
        assertNull(response.getMessage());
        assertEquals(0, response.getTotalRecordsMsDb());
        assertNull(response.getServiceResults());
        assertNull(response.getError());
    }

    @Test
    void shouldSetAndGetAllFields() {
        ComparisonResponse response = new ComparisonResponse();
        ServiceResult sr = ServiceResult.builder().serviceId("S1").build();

        response.setStatus("SUCCESS");
        response.setMessage("Done");
        response.setTotalRecordsMsDb(5);
        response.setTotalRecordsSoiDb(5);
        response.setTotalComparedSuccessCount(4);
        response.setTotalComparedFailureCount(1);
        response.setConfigsProcessed(2);
        response.setServiceResults(List.of(sr));
        response.setError("some error");

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Done", response.getMessage());
        assertEquals(5, response.getTotalRecordsMsDb());
        assertEquals(5, response.getTotalRecordsSoiDb());
        assertEquals(4, response.getTotalComparedSuccessCount());
        assertEquals(1, response.getTotalComparedFailureCount());
        assertEquals(2, response.getConfigsProcessed());
        assertEquals(1, response.getServiceResults().size());
        assertEquals("some error", response.getError());
    }

    @Test
    void shouldBeEqual_whenSameValues() {
        ComparisonResponse r1 = ComparisonResponse.builder()
                .status("SUCCESS").message("msg").totalRecordsMsDb(1).build();
        ComparisonResponse r2 = ComparisonResponse.builder()
                .status("SUCCESS").message("msg").totalRecordsMsDb(1).build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldNotBeEqual_whenDifferentValues() {
        ComparisonResponse r1 = ComparisonResponse.builder().status("SUCCESS").build();
        ComparisonResponse r2 = ComparisonResponse.builder().status("ERROR").build();

        assertNotEquals(r1, r2);
    }

    @Test
    void shouldGenerateToString() {
        ComparisonResponse response = ComparisonResponse.builder()
                .status("SUCCESS").message("OK").build();

        String str = response.toString();
        assertTrue(str.contains("SUCCESS"));
        assertTrue(str.contains("OK"));
    }
}