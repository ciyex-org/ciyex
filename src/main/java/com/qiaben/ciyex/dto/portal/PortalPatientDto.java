package com.qiaben.ciyex.dto.portal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalPatientDto {

    private Long id;
    private Long portalUserId;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    private LocalDate dateOfBirth;
    private String gender;
    
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    private Long ehrPatientId;
    private String medicalRecordNumber;
}
