package com.ibm.tibco.controller;

import lombok.extern.slf4j.Slf4j;
import com.ibm.tibco.dto.ComparisonRequest;
import com.ibm.tibco.dto.ComparisonResponse;
import com.ibm.tibco.model.db1.ComparatorConfigEntity;
import com.ibm.tibco.model.db1.ComparatorHistoryEntity;
import com.ibm.tibco.service.ComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/compare")
@Slf4j
public class ComparisonController {

    @Autowired
    private ComparisonService comparisonService;

    @PostMapping("/execute")
    public ResponseEntity<ComparisonResponse> executeComparison(
            @RequestBody(required = false) ComparisonRequest request) {

        log.info("Received comparison request: {}", request);

        if (request == null) {
            request = ComparisonRequest.builder().build();
        }

        try {
            ComparisonResponse response = comparisonService.compareTableRecords(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing comparison request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ComparisonResponse.builder()
                            .status("ERROR")
                            .message("Failed to process comparison request")
                            .error(e.getMessage())
                            .build());
        }
    }



    @GetMapping("/configs/active")
    public ResponseEntity<List<ComparatorConfigEntity>> getActiveConfigs() {
        log.info("Fetching active configurations");

        try {
            return ResponseEntity.ok(comparisonService.getActiveConfigs());
        } catch (Exception e) {
            log.error("Error fetching active configs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/configs")
    public ResponseEntity<List<ComparatorConfigEntity>> getAllConfigs() {
        log.info("Fetching all configurations");

        try {
            return ResponseEntity.ok(comparisonService.getAllConfigs());
        } catch (Exception e) {
            log.error("Error fetching all configs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/configs")
    public ResponseEntity<?> addConfig(@RequestBody ComparatorConfigEntity config) {
        log.info("Adding new config - serviceId: {}, tableName: {}", config.getServiceId(), config.getTableName());

        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(comparisonService.addConfig(config));
        } catch (Exception e) {
            log.error("Error adding config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to add config: " + e.getMessage());
        }
    }

    @PutMapping("/configs/update")
    public ResponseEntity<?> updateConfig(@RequestBody ComparatorConfigEntity config) {
        log.info("Updating config ID: {}", config.getId());

        if (config.getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Config id is required");
        }

        try {
            return ResponseEntity.ok(comparisonService.updateConfig(config.getId(), config));
        } catch (RuntimeException e) {
            log.error("Config not found with ID: {}", config.getId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update config: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ComparatorHistoryEntity>> getComparisonHistory(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String tableName) {

        log.info("Fetching comparison history - serviceId: {}, tableName: {}", serviceId, tableName);

        try {
            List<ComparatorHistoryEntity> history = comparisonService.getComparisonHistory(serviceId, tableName);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching comparison history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/history")
    public ResponseEntity<String> clearComparisonHistory(
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String tableName) {

        log.info("Clearing comparison history - serviceId: {}, tableName: {}", serviceId, tableName);

        try {
            comparisonService.clearHistory(serviceId, tableName);
            return ResponseEntity.ok("Comparison history cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing comparison history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to clear comparison history: " + e.getMessage());
        }
    }

}