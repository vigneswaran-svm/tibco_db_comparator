package com.ibm.tibco.repository;

import com.ibm.tibco.model.db1.ComparatorHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComparatorHistoryRepository extends JpaRepository<ComparatorHistoryEntity, Long> {

    List<ComparatorHistoryEntity> findByServiceId(String serviceId);

    List<ComparatorHistoryEntity> findByTableName(String tableName);

    List<ComparatorHistoryEntity> findByServiceIdAndTableName(String serviceId, String tableName);

    @Query("SELECT h FROM ComparatorHistoryEntity h WHERE " +
           "(:serviceId IS NULL OR h.serviceId = :serviceId) " +
           "AND (:tableName IS NULL OR h.tableName = :tableName) " +
           "AND (:status IS NULL OR h.comparatorExecutionStatus = :status)")
    List<ComparatorHistoryEntity> findHistoryWithFilters(
            @Param("serviceId") String serviceId,
            @Param("tableName") String tableName,
            @Param("status") String status);
}