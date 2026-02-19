package com.ibm.tibco.model.db1;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ComparatorHistoryEntityTest {

    // ==================== @PrePersist ====================

    @Test
    void shouldSetTimestamps_onPrePersist() {
        ComparatorHistoryEntity entity = new ComparatorHistoryEntity();
        assertNull(entity.getCreatedAt());

        entity.onCreate();

        assertNotNull(entity.getCreatedAt());
    }

    // ==================== Builder ====================

    @Test
    void shouldBuildEntity_withAllFields() {
        LocalDateTime now = LocalDateTime.now();
        ComparatorHistoryEntity entity = ComparatorHistoryEntity.builder()
                .id(1L)
                .serviceId("SVC1")
                .tableName("TABLE1")
                .fieldsName("F1,F2")
                .comparatorExecutionStatus("MATCH")
                .msTableValues("{\"F1\":\"val1\"}")
                .soiTableValues("{\"F1\":\"val1\"}")
                .createdAt(now)
                .build();

        assertEquals(1L, entity.getId());
        assertEquals("SVC1", entity.getServiceId());
        assertEquals("TABLE1", entity.getTableName());
        assertEquals("F1,F2", entity.getFieldsName());
        assertEquals("MATCH", entity.getComparatorExecutionStatus());
        assertEquals("{\"F1\":\"val1\"}", entity.getMsTableValues());
        assertEquals("{\"F1\":\"val1\"}", entity.getSoiTableValues());
        assertEquals(now, entity.getCreatedAt());
    }

    // ==================== NoArgsConstructor ====================

    @Test
    void shouldCreateEmptyEntity_withNoArgsConstructor() {
        ComparatorHistoryEntity entity = new ComparatorHistoryEntity();

        assertNull(entity.getId());
        assertNull(entity.getServiceId());
        assertNull(entity.getTableName());
        assertNull(entity.getFieldsName());
        assertNull(entity.getComparatorExecutionStatus());
        assertNull(entity.getMsTableValues());
        assertNull(entity.getSoiTableValues());
        assertNull(entity.getCreatedAt());
    }

    // ==================== Setters ====================

    @Test
    void shouldSetAndGetAllFields() {
        ComparatorHistoryEntity entity = new ComparatorHistoryEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setId(10L);
        entity.setServiceId("HISTORY_SVC");
        entity.setTableName("HISTORY_TABLE");
        entity.setFieldsName("FIELD_X,FIELD_Y");
        entity.setComparatorExecutionStatus("MISMATCH");
        entity.setMsTableValues("{\"X\":\"1\"}");
        entity.setSoiTableValues("{\"X\":\"2\"}");
        entity.setCreatedAt(now);

        assertEquals(10L, entity.getId());
        assertEquals("HISTORY_SVC", entity.getServiceId());
        assertEquals("HISTORY_TABLE", entity.getTableName());
        assertEquals("FIELD_X,FIELD_Y", entity.getFieldsName());
        assertEquals("MISMATCH", entity.getComparatorExecutionStatus());
        assertEquals("{\"X\":\"1\"}", entity.getMsTableValues());
        assertEquals("{\"X\":\"2\"}", entity.getSoiTableValues());
        assertEquals(now, entity.getCreatedAt());
    }

    // ==================== Equals & HashCode ====================

    @Test
    void shouldBeEqual_whenSameFields() {
        ComparatorHistoryEntity e1 = ComparatorHistoryEntity.builder()
                .id(1L).serviceId("SVC").tableName("T").fieldsName("F")
                .comparatorExecutionStatus("MATCH")
                .msTableValues("{}").soiTableValues("{}").build();
        ComparatorHistoryEntity e2 = ComparatorHistoryEntity.builder()
                .id(1L).serviceId("SVC").tableName("T").fieldsName("F")
                .comparatorExecutionStatus("MATCH")
                .msTableValues("{}").soiTableValues("{}").build();

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void shouldNotBeEqual_whenDifferentStatus() {
        ComparatorHistoryEntity e1 = ComparatorHistoryEntity.builder()
                .id(1L).comparatorExecutionStatus("MATCH").build();
        ComparatorHistoryEntity e2 = ComparatorHistoryEntity.builder()
                .id(1L).comparatorExecutionStatus("MISMATCH").build();

        assertNotEquals(e1, e2);
    }

    // ==================== ToString ====================

    @Test
    void shouldGenerateToString() {
        ComparatorHistoryEntity entity = ComparatorHistoryEntity.builder()
                .id(1L).serviceId("SVC1").comparatorExecutionStatus("MATCH").build();

        String str = entity.toString();
        assertTrue(str.contains("SVC1"));
        assertTrue(str.contains("MATCH"));
    }
}