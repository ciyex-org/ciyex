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
                .userId(user != null ? user.getId() : null)
                .firstName(user != null ? user.getFirstName() : null)
                .lastName(user != null ? user.getLastName() : null)
                .dob(patient.getDateOfBirth())
                .gender(patient.getGender())
                .phone(user != null ? user.getPhoneNumber() : null)
                .email(user != null ? user.getEmail() : null)
                .address(patient.getAddressLine1())
                .city(patient.getCity())
                .state(patient.getState())
                .country(patient.getCountry())
                .postalCode(patient.getPostalCode())
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
                .dateOfBirth(dto.getDob())
                .gender(dto.getGender())
                .addressLine1(dto.getAddress())
                .city(dto.getCity())
                .state(dto.getState())
                .country(dto.getCountry())
                .postalCode(dto.getPostalCode())
                .build();
    }

    /**
     * Update existing entity from DTO
     */
    public void updateEntityFromDto(PortalPatientDto dto, PortalPatient patient) {
        if (dto == null || patient == null) return;

        if (dto.getDob() != null) patient.setDateOfBirth(dto.getDob());
        if (dto.getGender() != null) patient.setGender(dto.getGender());
        if (dto.getAddress() != null) patient.setAddressLine1(dto.getAddress());
        if (dto.getCity() != null) patient.setCity(dto.getCity());
        if (dto.getState() != null) patient.setState(dto.getState());
        if (dto.getCountry() != null) patient.setCountry(dto.getCountry());
        if (dto.getPostalCode() != null) patient.setPostalCode(dto.getPostalCode());
    }
}
