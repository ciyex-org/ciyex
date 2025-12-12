package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class LabOrderDto {
    private Long id;
    private String externalId;
    private String fhirId; // FHIR resource id

    // Patient linkage
    private Long patientId;
    // Removed patientExternalId, mrn, patientFirstName, patientLastName, patientHomePhone
    private String physicianName;
    private String orderDateTime;
    private String orderName;
    private String labName;

    // Technical fields
    /**
     * Mandatory: unique order number supplied by upstream system or generated internally.
     * Must not be null/blank when creating or updating a Lab Order.
     */
    
    private String orderNumber;
    private String testCode;
    private String testDisplay;
    private String status;
    private String priority;
    private String orderDate;
    private String specimenId;
    private String orderingProvider;
    private String notes;

    // UPDATED
    
    private String diagnosisCode;   // replaces icdId
    private String procedureCode;   // new

    private String result;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
