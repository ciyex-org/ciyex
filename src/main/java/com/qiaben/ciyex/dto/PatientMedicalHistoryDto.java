package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class PatientMedicalHistoryDto {
    private Long id;                // DB id (for reads)
    private String externalId;      // FHIR id (optional)
    private Long orgId;             // tenant
    private Long patientId;
    private Long encounterId;

    private String description;     // the medical history text

    private Audit audit;

    @Data
    public static class Audit {
        // Use yyyy-MM-dd (string) in DTO to align with your preference
        private String createdDate;
        private String lastModifiedDate;
    }
}
