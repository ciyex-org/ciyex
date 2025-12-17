package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ReferralProviderDto {
    private Long id;
    private String name;
    private String specialty;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phoneNumber;
    private String email;
    private String fhirId;
    private String npiId;
    private String taxId;
    private Long practiceId; // Direct practice ID field for convenience
    private PracticeInfo practice;

    @Data
    public static class PracticeInfo {
        private Long id;
        private String name;
    }

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
    private Audit audit = new Audit();
}
