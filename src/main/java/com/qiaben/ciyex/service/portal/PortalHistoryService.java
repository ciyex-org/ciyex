package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalHistoryDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.PatientMedicalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalHistoryService {

    private final PatientMedicalHistoryService patientMedicalHistoryService;
    private final PortalUserRepository userRepository;

    /**
     * Get medical history for a portal patient by patient ID
     */
    public List<PortalHistoryDto.HistoryItem> getHistoryByPatientId(Long patientId) {
        try {
            // Get medical history from the main history service
            var historyList = patientMedicalHistoryService.getAllByPatient(patientId);
            if (historyList != null) {
                return historyList.stream()
                        .map(this::toPortalHistoryItem)
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error retrieving medical history for patient: {}", patientId, e);
            return List.of();
        }
    }

    /**
     * Get medical history for a portal user by email
     */
    public ApiResponse<List<PortalHistoryDto.HistoryItem>> getHistoryByEmail(String email) {
        try {
            PortalUser portalUser = userRepository.findByEmail(email)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<List<PortalHistoryDto.HistoryItem>>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<List<PortalHistoryDto.HistoryItem>>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            // Get the EHR patient ID from portal patient
            if (portalUser.getPortalPatient() == null || portalUser.getPortalPatient().getEhrPatientId() == null) {
                return ApiResponse.<List<PortalHistoryDto.HistoryItem>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .build();
            }

            Long ehrPatientId = portalUser.getPortalPatient().getEhrPatientId();

            List<PortalHistoryDto.HistoryItem> history = getHistoryByPatientId(ehrPatientId);

            return ApiResponse.<List<PortalHistoryDto.HistoryItem>>builder()
                    .success(true)
                    .message("Medical history retrieved successfully")
                    .data(history)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving medical history for user: {}", email, e);
            return ApiResponse.<List<PortalHistoryDto.HistoryItem>>builder()
                    .success(false)
                    .message("Failed to retrieve medical history")
                    .build();
        }
    }

    /**
     * Convert PatientMedicalHistoryDto to PortalHistoryDto.HistoryItem
     */
    private PortalHistoryDto.HistoryItem toPortalHistoryItem(com.qiaben.ciyex.dto.PatientMedicalHistoryDto dto) {
        PortalHistoryDto.HistoryItem item = new PortalHistoryDto.HistoryItem();
        item.setId(dto.getId());
        item.setMedicalCondition(dto.getMedicalCondition());
        item.setConditionName(dto.getConditionName());
        item.setStatus(dto.getStatus());
        item.setIsChronic(dto.getIsChronic());
        item.setDiagnosisDate(dto.getDiagnosisDate());
        item.setOnsetDate(dto.getOnsetDate());
        item.setResolvedDate(dto.getResolvedDate());
        item.setTreatmentDetails(dto.getTreatmentDetails());
        item.setDiagnosisDetails(dto.getDiagnosisDetails());
        item.setNotes(dto.getNotes());
        item.setDescription(dto.getDescription());
        return item;
    }
}