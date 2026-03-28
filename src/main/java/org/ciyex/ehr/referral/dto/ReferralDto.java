package org.ciyex.ehr.referral.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReferralDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String referringProvider;
    private String specialistName;
    private String specialistNpi;
    private String specialty;
    private String facilityName;
    private String facilityAddress;
    private String facilityPhone;
    private String facilityFax;
    private String reason;
    private String clinicalNotes;
    private String urgency;
    private String status;
    private String referralDate;
    private String expiryDate;
    private String authorizationNumber;
    private String insuranceName;
    private String insuranceId;
    private String appointmentDate;
    private String appointmentNotes;
    private String followUpNotes;
    private String createdAt;
    private String updatedAt;
}
