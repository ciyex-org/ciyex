package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.entity.portal.PortalPatient;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.portal.PortalPatientRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Get patient's own information (for portal dashboard)
     * Creates a basic patient profile if one doesn't exist
     */
    @Transactional
    public ApiResponse<PortalPatientDto> getPatientInfo(String email) {
        try {
            PortalUser portalUser = userRepository.findByEmail(email)
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

            PortalPatient patient = patientRepository.findByPortalUser_Id(portalUser.getId())
                    .orElse(null);

            // If patient profile doesn't exist, create a basic one
            if (patient == null) {
                log.info("Creating basic patient profile for portal user: {}", portalUser.getEmail());
                patient = createBasicPatientProfile(portalUser);
            }

            // ✅ Auto-link portal patient to EHR if a matching record exists
            linkToEhrPatientIfExists(patient);

            PortalPatientDto dto = PortalPatientDto.fromEntity(patient, portalUser);

            return ApiResponse.<PortalPatientDto>builder()
                    .success(true)
                    .message("Patient information retrieved successfully")
                    .data(dto)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving patient info for user: {}", email, e);
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
    public ApiResponse<PortalPatientDto> updatePatientInfo(String email, PortalPatientDto updateDto) {
        try {
            PortalUser portalUser = userRepository.findByEmail(email)
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

            PortalPatient patient = patientRepository.findByPortalUser_Id(portalUser.getId())
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
            log.error("Error updating patient info for user: {}", email, e);
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

    /**
     * Auto-link portal patient to EHR if matching record exists
     */
    @Transactional
    public void linkToEhrPatientIfExists(PortalPatient portalPatient) {
        try {
            // Only attempt linking if not already linked
            if (portalPatient.getEhrPatientId() != null) {
                log.info("Portal patient {} already linked to EHR ID {}", portalPatient.getId(), portalPatient.getEhrPatientId());
                return;
            }

            String email = portalPatient.getPortalUser().getEmail();

            // Check if EHR patient exists with same email
            Object ehrPatientIdObj = entityManager
                    .createNativeQuery("SELECT id FROM patients WHERE LOWER(email) = LOWER(?) LIMIT 1")
                    .setParameter(1, email)
                    .getSingleResult();

            if (ehrPatientIdObj != null) {
                Long ehrPatientId = ((Number) ehrPatientIdObj).longValue();
                portalPatient.setEhrPatientId(ehrPatientId);
                patientRepository.save(portalPatient);

                log.info("✅ Linked PortalPatient ID {} → EHR Patient ID {}", portalPatient.getId(), ehrPatientId);
            } else {
                log.warn("⚠️ No matching EHR patient found for email {}", email);
            }

        } catch (jakarta.persistence.NoResultException e) {
            log.warn("⚠️ No matching EHR patient found for email {}", portalPatient.getPortalUser().getEmail());
        } catch (Exception e) {
            log.error("❌ Error linking portal patient to EHR patient", e);
        }
    }
}
