package com.qiaben.ciyex.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientMedicalHistoryDto {
    private Long id;                 // DB ID
    private String externalId;       // FHIR Condition ID
    private Long orgId;              // Tenant
    private Long patientId;
    private Long encounterId;

    // Optional: if you have these from FHIR already; used by FHIR mapping
    private String patientExternalId;
    private String encounterExternalId;

    private String condition;        // e.g., "Hypertension"
    private String notes;            // free text notes
    private String status;           // e.g., "active", "resolved"
    private String onsetDate;        // yyyy-MM-dd
    private String resolvedDate;     // yyyy-MM-dd

    private Audit audit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Audit {
        private String createdDate;      // yyyy-MM-dd
        private String lastModifiedDate; // yyyy-MM-dd
    }
}
