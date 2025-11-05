package com.qiaben.ciyex.dto;

import lombok.Data;

/**
 * Data Transfer Object for LabResult.
 */
@Data
public class LabResultDto {
    private Long id;
    private String externalId; // FHIR Observation id
    private Long patientId;
    private String orderDate;
    private String procedureName;
    private String reportedDate;
    private String collectedDate;
    private String specimen;
    private String status;
    private String code;
    private String testName;
    private String resultDate;
    private String endDate;
    private String abnormalFlag;
    private String value;
    private String units;
    private String referenceRange;
    private String recommendations;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
