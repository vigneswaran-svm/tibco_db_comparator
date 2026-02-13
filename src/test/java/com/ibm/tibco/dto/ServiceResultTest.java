package com.ibm.tibco.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceResultTest {

    @Test
    void shouldBuildWithAllFields() {
        ServiceResult result = ServiceResult.builder()
                .serviceId("SVC1")
                .totalRecordsExecuted(10)
                .successCount(7)
                .failureCount(3)
                .build();

        assertEquals("SVC1", result.getServiceId());
        assertEquals(10, result.getTotalRecordsExecuted());
        assertEquals(7, result.getSuccessCount());
        assertEquals(3, result.getFailureCount());
    }

    @Test
    void shouldCreateWithNoArgsConstructor() {
        ServiceResult result = new ServiceResult();

        assertNull(result.getServiceId());
        assertEquals(0, result.getTotalRecordsExecuted());
        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        ServiceResult result = new ServiceResult("SVC1", 5, 3, 2);

        assertEquals("SVC1", result.getServiceId());
        assertEquals(5, result.getTotalRecordsExecuted());
        assertEquals(3, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());
    }

    @Test
    void shouldSetAndGetFields() {
        ServiceResult result = new ServiceResult();
        result.setServiceId("MY_SVC");
        result.setTotalRecordsExecuted(20);
        result.setSuccessCount(15);
        result.setFailureCount(5);

        assertEquals("MY_SVC", result.getServiceId());
        assertEquals(20, result.getTotalRecordsExecuted());
        assertEquals(15, result.getSuccessCount());
        assertEquals(5, result.getFailureCount());
    }

    @Test
    void shouldBeEqual_whenSameValues() {
        ServiceResult r1 = ServiceResult.builder()
                .serviceId("SVC").totalRecordsExecuted(10)
                .successCount(8).failureCount(2).build();
        ServiceResult r2 = ServiceResult.builder()
                .serviceId("SVC").totalRecordsExecuted(10)
                .successCount(8).failureCount(2).build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldNotBeEqual_whenDifferentValues() {
        ServiceResult r1 = ServiceResult.builder().serviceId("SVC1").successCount(5).build();
        ServiceResult r2 = ServiceResult.builder().serviceId("SVC1").successCount(3).build();

        assertNotEquals(r1, r2);
    }

    @Test
    void shouldNotBeEqual_toNull() {
        ServiceResult r1 = ServiceResult.builder().serviceId("SVC").build();

        assertNotEquals(r1, null);
    }

    @Test
    void shouldNotBeEqual_toDifferentType() {
        ServiceResult r1 = ServiceResult.builder().serviceId("SVC").build();

        assertNotEquals(r1, "a string");
    }

    @Test
    void shouldGenerateToString() {
        ServiceResult result = ServiceResult.builder()
                .serviceId("SVC1").totalRecordsExecuted(10)
                .successCount(8).failureCount(2).build();

        String str = result.toString();
        assertTrue(str.contains("SVC1"));
        assertTrue(str.contains("10"));
        assertTrue(str.contains("8"));
        assertTrue(str.contains("2"));
    }

    @Test
    void shouldAccumulateCounts() {
        ServiceResult result = ServiceResult.builder().serviceId("SVC1").build();

        result.setTotalRecordsExecuted(result.getTotalRecordsExecuted() + 5);
        result.setSuccessCount(result.getSuccessCount() + 3);
        result.setFailureCount(result.getFailureCount() + 2);

        assertEquals(5, result.getTotalRecordsExecuted());
        assertEquals(3, result.getSuccessCount());
        assertEquals(2, result.getFailureCount());

        // Accumulate again
        result.setTotalRecordsExecuted(result.getTotalRecordsExecuted() + 3);
        result.setSuccessCount(result.getSuccessCount() + 2);
        result.setFailureCount(result.getFailureCount() + 1);

        assertEquals(8, result.getTotalRecordsExecuted());
        assertEquals(5, result.getSuccessCount());
        assertEquals(3, result.getFailureCount());
    }
}