package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class PastMedicalHistoryDto {
    private Long id;             // DB id
    private String externalId;   // FHIR id (optional)
    private Long orgId;          // tenant
    private Long patientId;
    private Long encounterId;

    private String description;  // PMH narrative

    private Audit audit;

    @Data
    public static class Audit {
        // Keep strings to align with yyyy-MM-dd preference
        private String createdDate;
        private String lastModifiedDate;
    }
}
