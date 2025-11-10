package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalAllergyDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.AllergyIntoleranceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalAllergyService {

    private final AllergyIntoleranceService allergyIntoleranceService;
    private final PortalUserRepository userRepository;

    /**
     * Get allergies for a portal patient by patient ID
     */
    public List<PortalAllergyDto.AllergyItem> getAllergiesByPatientId(Long patientId) {
        try {
            // Get allergies from the main allergy service
            var allergyDto = allergyIntoleranceService.getByPatientId(patientId);
            if (allergyDto != null && allergyDto.getAllergiesList() != null) {
                return allergyDto.getAllergiesList().stream()
                        .map(this::toPortalAllergyItem)
                        .collect(Collectors.toList());
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error retrieving allergies for patient: {}", patientId, e);
            return List.of();
        }
    }

    /**
     * Get allergies for a portal user by email
     */
    public ApiResponse<List<PortalAllergyDto.AllergyItem>> getAllergiesByEmail(String email) {
        try {
            PortalUser portalUser = userRepository.findByEmail(email)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<List<PortalAllergyDto.AllergyItem>>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<List<PortalAllergyDto.AllergyItem>>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            // Get the EHR patient ID from portal patient
            if (portalUser.getPortalPatient() == null || portalUser.getPortalPatient().getEhrPatientId() == null) {
                return ApiResponse.<List<PortalAllergyDto.AllergyItem>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .build();
            }

            Long ehrPatientId = portalUser.getPortalPatient().getEhrPatientId();

            List<PortalAllergyDto.AllergyItem> allergies = getAllergiesByPatientId(ehrPatientId);

            return ApiResponse.<List<PortalAllergyDto.AllergyItem>>builder()
                    .success(true)
                    .message("Allergies retrieved successfully")
                    .data(allergies)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving allergies for user: {}", email, e);
            return ApiResponse.<List<PortalAllergyDto.AllergyItem>>builder()
                    .success(false)
                    .message("Failed to retrieve allergies")
                    .build();
        }
    }

    /**
     * Convert AllergyIntoleranceDto.AllergyItem to PortalAllergyDto.AllergyItem
     */
    private PortalAllergyDto.AllergyItem toPortalAllergyItem(com.qiaben.ciyex.dto.AllergyIntoleranceDto.AllergyItem item) {
        PortalAllergyDto.AllergyItem portalItem = new PortalAllergyDto.AllergyItem();
        portalItem.setId(item.getId());
        portalItem.setAllergyName(item.getAllergyName());
        portalItem.setReaction(item.getReaction());
        portalItem.setSeverity(item.getSeverity());
        portalItem.setStatus(item.getStatus());
        portalItem.setStartDate(item.getStartDate());
        portalItem.setEndDate(item.getEndDate());
        portalItem.setComments(item.getComments());
        return portalItem;
    }
}