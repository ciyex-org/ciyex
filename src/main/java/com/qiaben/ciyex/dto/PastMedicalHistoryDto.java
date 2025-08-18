package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class PastMedicalHistoryDto {
    private Long id;
    private String externalId;     // External storage (FHIR) id
    private Long orgId;            // Tenant
    private Long patientId;
    private Long encounterId;

    private String condition;      // e.g., "Hypertension"
    private String notes;          // free text notes
    private String status;         // e.g., "active", "resolved", etc.
    private String onsetDate;      // optional, ISO date string: yyyy-MM-dd
    private String resolvedDate;   // optional, ISO date string: yyyy-MM-dd

    private String createdDate;    // ISO datetime string
    private String lastModifiedDate;
}
