package com.ibm.tibco.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonRequestTest {

    @Test
    void shouldBuildWithAllFields() {
        ComparisonRequest request = ComparisonRequest.builder()
                .serviceName("SVC1")
                .tableName("TABLE1")
                .build();

        assertEquals("SVC1", request.getServiceName());
        assertEquals("TABLE1", request.getTableName());
    }

    @Test
    void shouldBuildWithNoFields() {
        ComparisonRequest request = ComparisonRequest.builder().build();

        assertNull(request.getServiceName());
        assertNull(request.getTableName());
    }

    @Test
    void shouldCreateWithNoArgsConstructor() {
        ComparisonRequest request = new ComparisonRequest();

        assertNull(request.getServiceName());
        assertNull(request.getTableName());
    }

    @Test
    void shouldCreateWithAllArgsConstructor() {
        ComparisonRequest request = new ComparisonRequest("SVC1", "TABLE1");

        assertEquals("SVC1", request.getServiceName());
        assertEquals("TABLE1", request.getTableName());
    }

    @Test
    void shouldSetAndGetFields() {
        ComparisonRequest request = new ComparisonRequest();
        request.setServiceName("MY_SVC");
        request.setTableName("MY_TABLE");

        assertEquals("MY_SVC", request.getServiceName());
        assertEquals("MY_TABLE", request.getTableName());
    }

    @Test
    void shouldBeEqual_whenSameValues() {
        ComparisonRequest r1 = ComparisonRequest.builder()
                .serviceName("SVC").tableName("T").build();
        ComparisonRequest r2 = ComparisonRequest.builder()
                .serviceName("SVC").tableName("T").build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldNotBeEqual_whenDifferentValues() {
        ComparisonRequest r1 = ComparisonRequest.builder().serviceName("SVC1").build();
        ComparisonRequest r2 = ComparisonRequest.builder().serviceName("SVC2").build();

        assertNotEquals(r1, r2);
    }

    @Test
    void shouldGenerateToString() {
        ComparisonRequest request = ComparisonRequest.builder()
                .serviceName("SVC1").tableName("TABLE1").build();

        String str = request.toString();
        assertTrue(str.contains("SVC1"));
        assertTrue(str.contains("TABLE1"));
    }
}