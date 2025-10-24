package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class LabOrderDto {
    private Long id;
    private String tenantName;

    // Patient linkage
    private Long patientId;
    private String patientExternalId;

    // EMR/UI fields
    private String mrn;
    private String encounterId;
    private String physicianName;
    private String patientFirstName;
    private String patientLastName;
    private String patientHomePhone;
    private String orderDateTime;
    private String orderName;
    private String labName;

    // Technical fields for FHIR/code mapping
    private String orderNumber;
    private String testCode;
    private String testDisplay;
    private String status;
    private String priority;
    private String orderDate;
    private String specimenId;
    private String orderingProvider;
    private String notes;

    // NEW
    private String icdId;   // e.g., ICD-10-CM code like "E11.9"
    private String result;  // free-text or structured JSON as a String for now

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
