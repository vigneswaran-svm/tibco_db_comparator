package com.ibm.tibco.repository;

import com.ibm.tibco.model.db1.ComparatorConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComparatorConfigRepository extends JpaRepository<ComparatorConfigEntity, Long> {

    List<ComparatorConfigEntity> findByComparatorExecutionStatus(String status);

    @Query("SELECT c FROM ComparatorConfigEntity c WHERE " +
           "c.comparatorExecutionStatus = 'N' " +
           "AND (:serviceId IS NULL OR c.serviceId = :serviceId) " +
           "AND (:tableName IS NULL OR c.tableName = :tableName)")
    List<ComparatorConfigEntity> findActiveConfigsWithFilters(
            @Param("serviceId") String serviceId,
            @Param("tableName") String tableName);
}