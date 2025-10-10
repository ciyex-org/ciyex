package com.qiaben.ciyex.dto.portal;

import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;
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
    private Long portalUserId;   // Link to PortalUser.id

    // User information
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    // Patient information
    private LocalDate dateOfBirth;
    private String gender;
    
    // Address information
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // EHR linkage
    private Long ehrPatientId;
    private String medicalRecordNumber;

    public static PortalPatientDto fromEntity(PortalPatient patient, PortalUser user) {
        return PortalPatientDto.builder()
                .id(patient.getId())
                .portalUserId(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .addressLine1(patient.getAddressLine1())
                .addressLine2(patient.getAddressLine2())
                .city(patient.getCity())
                .state(patient.getState())
                .postalCode(patient.getPostalCode())
                .country(patient.getCountry())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .emergencyContactRelationship(patient.getEmergencyContactRelationship())
                .ehrPatientId(patient.getEhrPatientId())
                .medicalRecordNumber(patient.getMedicalRecordNumber())
                .build();
    }
}
