package com.qiaben.ciyex.mapper.portal;

import org.springframework.stereotype.Component;

import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;

@Component
public class PortalPatientMapper {

    /**
     * Convert entity -> DTO
     */
    public PortalPatientDto toDto(PortalPatient patient) {
        if (patient == null) return null;

        PortalUser user = patient.getPortalUser();

        return PortalPatientDto.builder()
                .id(patient.getId())
                .portalUserId(user != null ? user.getId() : null)
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .email(user != null ? user.getEmail() : null)
                .phoneNumber(user != null ? user.getPhoneNumber() : null)
                .dateOfBirth(patient.getDateOfBirth())
                .gender(patient.getGender())
                .addressLine1(patient.getAddressLine1())
                .addressLine2(patient.getAddressLine2())
                .city(patient.getCity())
                .state(patient.getState())
                .country(patient.getCountry())
                .postalCode(patient.getPostalCode())
                .emergencyContactName(patient.getEmergencyContactName())
                .emergencyContactPhone(patient.getEmergencyContactPhone())
                .emergencyContactRelationship(patient.getEmergencyContactRelationship())
                .ehrPatientId(patient.getEhrPatientId())
                .medicalRecordNumber(patient.getMedicalRecordNumber())
                .build();
    }

    /**
     * Convert DTO -> entity (with linked PortalUser)
     */
    public PortalPatient toEntity(PortalPatientDto dto, PortalUser user) {
        if (dto == null) return null;

        return PortalPatient.builder()
                .id(dto.getId())
                .portalUser(user)  // maintain the link back to PortalUser
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .postalCode(dto.getPostalCode())
                .emergencyContactName(dto.getEmergencyContactName())
                .emergencyContactPhone(dto.getEmergencyContactPhone())
                .emergencyContactRelationship(dto.getEmergencyContactRelationship())
                .ehrPatientId(dto.getEhrPatientId())
                .medicalRecordNumber(dto.getMedicalRecordNumber())
                .build();
    }

    /**
     * Update existing entity from DTO
     */
    public void updateEntityFromDto(PortalPatientDto dto, PortalPatient patient) {
        if (dto == null || patient == null) return;

        if (dto.getDateOfBirth() != null) patient.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getGender() != null) patient.setGender(dto.getGender());
        if (dto.getAddressLine1() != null) patient.setAddressLine1(dto.getAddressLine1());
        if (dto.getAddressLine2() != null) patient.setAddressLine2(dto.getAddressLine2());
        if (dto.getCity() != null) patient.setCity(dto.getCity());
        if (dto.getState() != null) patient.setState(dto.getState());
        if (dto.getCountry() != null) patient.setCountry(dto.getCountry());
        if (dto.getPostalCode() != null) patient.setPostalCode(dto.getPostalCode());
        if (dto.getEmergencyContactName() != null) patient.setEmergencyContactName(dto.getEmergencyContactName());
        if (dto.getEmergencyContactPhone() != null) patient.setEmergencyContactPhone(dto.getEmergencyContactPhone());
        if (dto.getEmergencyContactRelationship() != null) patient.setEmergencyContactRelationship(dto.getEmergencyContactRelationship());
        if (dto.getEhrPatientId() != null) patient.setEhrPatientId(dto.getEhrPatientId());
        if (dto.getMedicalRecordNumber() != null) patient.setMedicalRecordNumber(dto.getMedicalRecordNumber());
    }
}
