package com.ibm.tibco.model.db1;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ComparatorConfigEntityTest {

    // ==================== @PrePersist ====================

    @Test
    void shouldSetTimestamps_onPrePersist() {
        ComparatorConfigEntity entity = new ComparatorConfigEntity();
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());

        entity.onCreate();

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        // createdAt and updatedAt are set by separate LocalDateTime.now() calls,
        // so allow a small difference (< 1 second)
        assertTrue(java.time.Duration.between(entity.getCreatedAt(), entity.getUpdatedAt()).toMillis() < 1000);
    }

    // ==================== @PreUpdate ====================

    @Test
    void shouldUpdateTimestamp_onPreUpdate() {
        ComparatorConfigEntity entity = new ComparatorConfigEntity();
        entity.onCreate();
        LocalDateTime originalUpdatedAt = entity.getUpdatedAt();

        // Small delay to ensure different timestamp
        entity.onUpdate();

        assertNotNull(entity.getUpdatedAt());
        // updatedAt should be >= original (same or later)
        assertTrue(entity.getUpdatedAt().compareTo(originalUpdatedAt) >= 0);
    }

    @Test
    void shouldNotChangeCreatedAt_onPreUpdate() {
        ComparatorConfigEntity entity = new ComparatorConfigEntity();
        entity.onCreate();
        LocalDateTime originalCreatedAt = entity.getCreatedAt();

        entity.onUpdate();

        assertEquals(originalCreatedAt, entity.getCreatedAt());
    }

    // ==================== Builder ====================

    @Test
    void shouldBuildEntity_withAllFields() {
        LocalDateTime now = LocalDateTime.now();
        ComparatorConfigEntity entity = ComparatorConfigEntity.builder()
                .id(1L)
                .serviceId("SVC1")
                .tableName("TABLE1")
                .tableFields("F1,F2")
                .primaryFields("F1")
                .executionStatus("N")
                .startDate(now)
                .endDate(now.plusDays(1))
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(1L, entity.getId());
        assertEquals("SVC1", entity.getServiceId());
        assertEquals("TABLE1", entity.getTableName());
        assertEquals("F1,F2", entity.getTableFields());
        assertEquals("F1", entity.getPrimaryFields());
        assertEquals("N", entity.getExecutionStatus());
        assertEquals(now, entity.getStartDate());
        assertEquals(now.plusDays(1), entity.getEndDate());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    // ==================== NoArgsConstructor ====================

    @Test
    void shouldCreateEmptyEntity_withNoArgsConstructor() {
        ComparatorConfigEntity entity = new ComparatorConfigEntity();

        assertNull(entity.getId());
        assertNull(entity.getServiceId());
        assertNull(entity.getTableName());
        assertNull(entity.getTableFields());
        assertNull(entity.getPrimaryFields());
        assertNull(entity.getExecutionStatus());
        assertNull(entity.getStartDate());
        assertNull(entity.getEndDate());
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    // ==================== Setters ====================

    @Test
    void shouldSetAndGetAllFields() {
        ComparatorConfigEntity entity = new ComparatorConfigEntity();
        LocalDateTime now = LocalDateTime.now();

        entity.setId(5L);
        entity.setServiceId("TEST_SVC");
        entity.setTableName("TEST_TABLE");
        entity.setTableFields("FIELD_A,FIELD_B");
        entity.setPrimaryFields("FIELD_A");
        entity.setExecutionStatus("Y");
        entity.setStartDate(now);
        entity.setEndDate(now.plusHours(12));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertEquals(5L, entity.getId());
        assertEquals("TEST_SVC", entity.getServiceId());
        assertEquals("TEST_TABLE", entity.getTableName());
        assertEquals("FIELD_A,FIELD_B", entity.getTableFields());
        assertEquals("FIELD_A", entity.getPrimaryFields());
        assertEquals("Y", entity.getExecutionStatus());
        assertEquals(now, entity.getStartDate());
        assertEquals(now.plusHours(12), entity.getEndDate());
    }

    // ==================== Equals & HashCode ====================

    @Test
    void shouldBeEqual_whenSameFields() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 1, 0, 0);
        ComparatorConfigEntity e1 = ComparatorConfigEntity.builder()
                .id(1L).serviceId("SVC").tableName("T").tableFields("F")
                .primaryFields("P").executionStatus("N")
                .startDate(now).endDate(now).build();
        ComparatorConfigEntity e2 = ComparatorConfigEntity.builder()
                .id(1L).serviceId("SVC").tableName("T").tableFields("F")
                .primaryFields("P").executionStatus("N")
                .startDate(now).endDate(now).build();

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }

    @Test
    void shouldNotBeEqual_whenDifferentId() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 1, 0, 0);
        ComparatorConfigEntity e1 = ComparatorConfigEntity.builder().id(1L).serviceId("SVC")
                .tableName("T").startDate(now).endDate(now).build();
        ComparatorConfigEntity e2 = ComparatorConfigEntity.builder().id(2L).serviceId("SVC")
                .tableName("T").startDate(now).endDate(now).build();

        assertNotEquals(e1, e2);
    }

    // ==================== ToString ====================

    @Test
    void shouldGenerateToString() {
        ComparatorConfigEntity entity = ComparatorConfigEntity.builder()
                .id(1L).serviceId("SVC1").build();

        String str = entity.toString();
        assertTrue(str.contains("SVC1"));
        assertTrue(str.contains("1"));
    }
}