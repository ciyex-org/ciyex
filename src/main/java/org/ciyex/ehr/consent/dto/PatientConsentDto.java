package org.ciyex.ehr.consent.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientConsentDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String consentType;
    private String status;
    private String signedDate;
    private String expiryDate;
    private String signedBy;
    private String witnessName;
    private String documentUrl;
    private String version;
    private String notes;
    private String createdAt;
    private String updatedAt;
}
