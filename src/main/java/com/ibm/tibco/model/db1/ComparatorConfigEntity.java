package com.ibm.tibco.model.db1;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "COMPARATOR_CONFIG")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparatorConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false, length = 100)
    private String serviceId;

    @Column(name = "table_name", nullable = false, length = 128)
    private String tableName;

    @Column(name = "table_fields", nullable = false, columnDefinition = "TEXT")
    private String tableFields;

    @Column(name = "primary_fields", nullable = false, columnDefinition = "TEXT")
    private String primaryFields;

    @Column(name = "execution_status", nullable = false, length = 1)
    private String executionStatus;

    @Column(name = "ms_service_name_whitelist", columnDefinition = "MEDIUMTEXT")
    private String msServiceNameWhitelist;

    @Column(name = "soi_service_name_whitelist", columnDefinition = "MEDIUMTEXT")
    private String soiServiceNameWhitelist;

    @Column(name = "where_date_field", length = 50)
    private String whereDateField;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
