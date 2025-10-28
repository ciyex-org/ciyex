package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalVitalsDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.VitalsRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalVitalsService {

    private final VitalsRepository vitalsRepository;
    private final PortalUserRepository userRepository;

    /**
     * Get recent vitals for a portal patient (last 10 records)
     */
    public ApiResponse<List<PortalVitalsDto>> getRecentVitals(String email) {
        try {
            PortalUser portalUser = userRepository.findByEmail(email)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<List<PortalVitalsDto>>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<List<PortalVitalsDto>>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            // Get the EHR patient ID from portal patient
            if (portalUser.getPortalPatient() == null || portalUser.getPortalPatient().getEhrPatientId() == null) {
                return ApiResponse.<List<PortalVitalsDto>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .build();
            }

            Long ehrPatientId = portalUser.getPortalPatient().getEhrPatientId();

            // Get recent vitals for this patient (using EHR patient ID)
            List<PortalVitalsDto> vitals = vitalsRepository.findByPatientIdOrderByRecordedAtDesc(ehrPatientId)
                    .stream()
                    .limit(10) // Last 10 records
                    .map(this::toPortalVitalsDto)
                    .collect(Collectors.toList());

            return ApiResponse.<List<PortalVitalsDto>>builder()
                    .success(true)
                    .message("Recent vitals retrieved successfully")
                    .data(vitals)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving recent vitals for user: {}", email, e);
            return ApiResponse.<List<PortalVitalsDto>>builder()
                    .success(false)
                    .message("Failed to retrieve recent vitals")
                    .build();
        }
    }

    /**
     * Get all vitals for a portal patient
     */
    public ApiResponse<List<PortalVitalsDto>> getAllVitals(String email) {
        try {
            PortalUser portalUser = userRepository.findByEmail(email)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<List<PortalVitalsDto>>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<List<PortalVitalsDto>>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            // Get the EHR patient ID from portal patient
            if (portalUser.getPortalPatient() == null || portalUser.getPortalPatient().getEhrPatientId() == null) {
                return ApiResponse.<List<PortalVitalsDto>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .build();
            }

            Long ehrPatientId = portalUser.getPortalPatient().getEhrPatientId();

            // Get all vitals for this patient
            List<PortalVitalsDto> vitals = vitalsRepository.findByPatientIdOrderByRecordedAtDesc(ehrPatientId)
                    .stream()
                    .map(this::toPortalVitalsDto)
                    .collect(Collectors.toList());

            return ApiResponse.<List<PortalVitalsDto>>builder()
                    .success(true)
                    .message("All vitals retrieved successfully")
                    .data(vitals)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving all vitals for user: {}", email, e);
            return ApiResponse.<List<PortalVitalsDto>>builder()
                    .success(false)
                    .message("Failed to retrieve vitals")
                    .build();
        }
    }

    /**
     * Convert Vitals entity to PortalVitalsDto
     */
    private PortalVitalsDto toPortalVitalsDto(com.qiaben.ciyex.entity.Vitals vitals) {
        return PortalVitalsDto.builder()
                .id(vitals.getId())
                .patientId(vitals.getPatientId())
                .encounterId(vitals.getEncounterId())
                .weightKg(vitals.getWeightKg())
                .weightLbs(vitals.getWeightLbs())
                .bpSystolic(vitals.getBpSystolic())
                .bpDiastolic(vitals.getBpDiastolic())
                .pulse(vitals.getPulse())
                .respiration(vitals.getRespiration())
                .temperatureC(vitals.getTemperatureC())
                .temperatureF(vitals.getTemperatureF())
                .oxygenSaturation(vitals.getOxygenSaturation())
                .bmi(vitals.getBmi())
                .notes(vitals.getNotes())
                .recordedAt(vitals.getRecordedAt())
                .createdDate(vitals.getCreatedDate())
                .build();
    }
}