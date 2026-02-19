package com.ibm.tibco.model.db1;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "COMPARATOR_HISTORY")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparatorHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false, length = 100)
    private String serviceId;

    @Column(name = "table_name", nullable = false, length = 128)
    private String tableName;

    @Column(name = "fields_name", nullable = false, columnDefinition = "TEXT")
    private String fieldsName;

    @Column(name = "comparator_execution_status", nullable = false, length = 20)
    private String comparatorExecutionStatus;

    @Column(name = "soi_table_values", nullable = false, columnDefinition = "TEXT")
    private String soiTableValues;

    @Column(name = "ms_table_values", nullable = false, columnDefinition = "TEXT")
    private String msTableValues;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}