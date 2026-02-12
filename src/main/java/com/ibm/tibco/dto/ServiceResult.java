package com.ibm.tibco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResult {

    private String serviceId;
    private int totalRecordsExecuted;
    private int successCount;
    private int failureCount;
}