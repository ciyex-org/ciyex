package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class RecallDto {
    private Long id;
    private Long patientId;
    private Long providerId;

    private String patientName;
    private String dob;

    // Flattened contact + address (to match frontend)
    private String phone;
    private String email;
    private String address;
    private String city;
    private String state;
    private String zip;

    private String lastVisit;
    private String recallDate;
    private String recallReason;

    private boolean smsConsent;
    private boolean emailConsent;

    private Audit audit;
    private String fhirId;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
