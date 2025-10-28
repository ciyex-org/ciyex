package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalPatientService {

    private final PortalPatientRepository patientRepository;
    private final PortalUserRepository userRepository;

    /**
     * Get patient's own information (for portal dashboard)
     * Creates a basic patient profile if one doesn't exist
     */
    @Transactional(readOnly = true)
    public ApiResponse<PortalPatientDto> getPatientInfo(Long portalUserId) {
        try {
            PortalUser portalUser = userRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            PortalPatient patient = patientRepository.findByPortalUser_Id(portalUserId)
                    .orElse(null);

            // If patient profile doesn't exist, create a basic one
            if (patient == null) {
                log.info("Creating basic patient profile for portal user: {}", portalUserId);
                patient = createBasicPatientProfile(portalUser);
            }

            PortalPatientDto dto = PortalPatientDto.fromEntity(patient, portalUser);

            return ApiResponse.<PortalPatientDto>builder()
                    .success(true)
                    .message("Patient information retrieved successfully")
                    .data(dto)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving patient info for user: {}", portalUserId, e);
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Failed to retrieve patient information")
                    .build();
        }
    }

    /**
     * Update patient's own information
     */
    @Transactional
    public ApiResponse<PortalPatientDto> updatePatientInfo(Long portalUserId, PortalPatientDto updateDto) {
        try {
            PortalUser portalUser = userRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            PortalPatient patient = patientRepository.findByPortalUser_Id(portalUserId)
                    .orElse(null);

            if (patient == null) {
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("Patient profile not found")
                        .build();
            }

            // Update patient fields
            if (updateDto.getAddressLine1() != null) patient.setAddressLine1(updateDto.getAddressLine1());
            if (updateDto.getAddressLine2() != null) patient.setAddressLine2(updateDto.getAddressLine2());
            if (updateDto.getCity() != null) patient.setCity(updateDto.getCity());
            if (updateDto.getState() != null) patient.setState(updateDto.getState());
            if (updateDto.getPostalCode() != null) patient.setPostalCode(updateDto.getPostalCode());
            if (updateDto.getCountry() != null) patient.setCountry(updateDto.getCountry());
            if (updateDto.getEmergencyContactName() != null) patient.setEmergencyContactName(updateDto.getEmergencyContactName());
            if (updateDto.getEmergencyContactPhone() != null) patient.setEmergencyContactPhone(updateDto.getEmergencyContactPhone());

            PortalPatient savedPatient = patientRepository.save(patient);
            PortalPatientDto dto = PortalPatientDto.fromEntity(savedPatient, portalUser);

            return ApiResponse.<PortalPatientDto>builder()
                    .success(true)
                    .message("Patient information updated successfully")
                    .data(dto)
                    .build();

        } catch (Exception e) {
            log.error("Error updating patient info for user: {}", portalUserId, e);
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Failed to update patient information")
                    .build();
        }
    }

    /**
     * Create a basic patient profile for a portal user
     */
    @Transactional
    public PortalPatient createBasicPatientProfile(PortalUser portalUser) {
        try {
            PortalPatient patient = PortalPatient.builder()
                    .portalUser(portalUser)
                    .dateOfBirth(LocalDate.now().minusYears(25)) // Default age 25
                    .country("USA")
                    .build();

            return patientRepository.save(patient);
        } catch (Exception e) {
            log.error("Error creating basic patient profile for user: {}", portalUser.getId(), e);
            throw new RuntimeException("Failed to create patient profile", e);
        }
    }
}