package com.ibm.tibco.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonResponse {

    private String status;
    private String message;
    private int totalRecordsMsDb;
    private int totalRecordsSoiDb;
    private int totalComparedSuccessCount;
    private int totalComparedFailureCount;
    private int configsProcessed;
    private List<ServiceResult> serviceResults;
    private String error;
}