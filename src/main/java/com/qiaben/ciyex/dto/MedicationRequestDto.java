package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class MedicationRequestDto {
    private Long id;
    private String fhirId;
    private String externalId;  // Alias for fhirId for external integrations
    private Long patientId;
    private Long encounterId;
    private String medicationName;
    private String dosage;
    private String instructions;
    private String dateIssued;
    private String prescribingDoctor;
    private String status;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
    private Audit audit;
}
