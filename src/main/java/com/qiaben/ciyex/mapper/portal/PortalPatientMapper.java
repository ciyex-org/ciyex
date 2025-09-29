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

        PortalUser user = patient.getUser();

        return PortalPatientDto.builder()
                .id(patient.getId())
                .userId(user != null ? user.getId() : null)
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dob(patient.getDob())
                .gender(patient.getGender())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .city(user != null ? user.getCity() : null)
                .state(user != null ? user.getState() : null)
                .country(user != null ? user.getCountry() : null)
                .postalCode(user != null ? user.getPostalCode() : null)
                .build();
    }

    /**
     * Convert DTO -> entity (with linked PortalUser)
     */
    public PortalPatient toEntity(PortalPatientDto dto, PortalUser user) {
        if (dto == null) return null;

        return PortalPatient.builder()
                .id(dto.getId())
                .user(user)  // 🔹 maintain the link back to PortalUser
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dob(dto.getDob())
                .gender(dto.getGender())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .build();
    }

    /**
     * Update existing entity from DTO
     */
    public void updateEntityFromDto(PortalPatientDto dto, PortalPatient patient) {
        if (dto == null || patient == null) return;

        if (dto.getFirstName() != null) patient.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) patient.setLastName(dto.getLastName());
        if (dto.getDob() != null) patient.setDob(dto.getDob());
        if (dto.getGender() != null) patient.setGender(dto.getGender());
        if (dto.getPhone() != null) patient.setPhone(dto.getPhone());
        if (dto.getEmail() != null) patient.setEmail(dto.getEmail());
        if (dto.getAddress() != null) patient.setAddress(dto.getAddress());
    }
}
