package org.ciyex.ehr.lab.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LabResultDto {
    private Long id;
    private Long labOrderId;
    private Long patientId;
    private Long encounterId;
    private String orderNumber;
    private String procedureName;
    private String testCode;
    private String testName;
    private String loincCode;
    private String status;
    private String specimen;
    private String collectedDate;
    private String reportedDate;
    private String abnormalFlag;
    private String value;
    private Double numericValue;
    private String units;
    private Double referenceLow;
    private Double referenceHigh;
    private String referenceRange;
    private String notes;
    private String recommendations;
    private Boolean signed;
    private String signedAt;
    private String signedBy;
    private String panelName;
    private String panelCode;
    private String createdAt;
    private String updatedAt;
}
