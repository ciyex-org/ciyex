package com.qiaben.ciyex.mapper.portal;

import org.springframework.stereotype.Component;

import com.qiaben.ciyex.dto.portal.dto.PortalPatientDto;
import com.qiaben.ciyex.entity.portal.entity.PortalPatient;
import com.qiaben.ciyex.entity.portal.entity.PortalUser;

@Component
public class PortalPatientMapper {

    /**
     * Convert entity -> DTO
     */
    public PortalPatientDto toDto(PortalPatient patient) {
        if (patient == null) return null;

        return PortalPatientDto.builder()
                .id(patient.getId())
                .userId(patient.getUser() != null ? patient.getUser().getId() : null)
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dob(patient.getDob())
                .gender(patient.getGender())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .insuranceId(patient.getInsuranceId())
                .build();
    }

    /**
     * Convert DTO -> entity (with linked PortalUser)
     */
    public PortalPatient toEntity(PortalPatientDto dto, PortalUser user) {
        if (dto == null) return null;

        return PortalPatient.builder()
                .id(dto.getId())
                .user(user)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .dob(dto.getDob())
                .gender(dto.getGender())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .insuranceId(dto.getInsuranceId())
                .build();
    }

    /**
     * Update existing entity from DTO
     */
    public void updateEntityFromDto(PortalPatientDto dto, PortalPatient patient) {
        if (dto == null || patient == null) return;

        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setDob(dto.getDob());
        patient.setGender(dto.getGender());
        patient.setPhone(dto.getPhone());
        patient.setEmail(dto.getEmail());
        patient.setAddress(dto.getAddress());
        patient.setInsuranceId(dto.getInsuranceId());
    }
}
