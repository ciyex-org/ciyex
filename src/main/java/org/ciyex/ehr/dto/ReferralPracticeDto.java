package org.ciyex.ehr.dto;

import lombok.Data;

@Data
public class ReferralPracticeDto {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phoneNumber;
    private String email;
    private String fhirId; // For external FHIR integration
    private String npiId;
    private String taxId;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
    private Audit audit;
}
