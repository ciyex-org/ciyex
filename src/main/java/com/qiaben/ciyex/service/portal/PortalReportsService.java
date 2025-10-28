package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.DocumentRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalReportsService {

    private final DocumentRepository documentRepository;
    private final PortalUserRepository userRepository;
    private final DocumentService documentService;

    /**
     * Get all reports for a portal patient (reports are documents categorized as reports)
     */
    public ApiResponse<List<DocumentDto>> getAllReports(Long portalUserId) {
        try {
            PortalUser portalUser = userRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            // Get the EHR patient ID from portal patient
            if (portalUser.getPortalPatient() == null || portalUser.getPortalPatient().getEhrPatientId() == null) {
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .build();
            }

            Long ehrPatientId = portalUser.getPortalPatient().getEhrPatientId();
            Long orgId = portalUser.getOrgId();

            // Get all documents for this patient and filter for report categories
            com.qiaben.ciyex.dto.ApiResponse<List<DocumentDto>> sharedResponse = documentService.getAllForPatient(orgId, ehrPatientId);
            if (sharedResponse.isSuccess() && sharedResponse.getData() != null) {
                List<DocumentDto> reports = sharedResponse.getData()
                        .stream()
                        .filter(doc -> isReportCategory(doc.getCategory()))
                        .collect(Collectors.toList());

                return ApiResponse.<List<DocumentDto>>builder()
                        .success(true)
                        .message("Reports retrieved successfully")
                        .data(reports)
                        .build();
            } else {
                // If no documents found, return empty reports list
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(true)
                        .message(sharedResponse.getMessage() != null ? sharedResponse.getMessage() : "No reports found")
                        .data(new ArrayList<>())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error retrieving reports for user: {}", portalUserId, e);
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Failed to retrieve reports")
                    .build();
        }
    }

    /**
     * Get recent reports for a portal patient (last 10 records)
     */
    public ApiResponse<List<DocumentDto>> getRecentReports(Long portalUserId) {
        try {
            ApiResponse<List<DocumentDto>> response = getAllReports(portalUserId);
            if (response.isSuccess() && response.getData() != null) {
                List<DocumentDto> recentReports = response.getData()
                        .stream()
                        .sorted((a, b) -> {
                            // Sort by some criteria - for now just return all
                            return 0;
                        })
                        .limit(10)
                        .collect(Collectors.toList());

                return ApiResponse.<List<DocumentDto>>builder()
                        .success(true)
                        .message("Recent reports retrieved successfully")
                        .data(recentReports)
                        .build();
            } else {
                return response;
            }

        } catch (Exception e) {
            log.error("Error retrieving recent reports for user: {}", portalUserId, e);
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Failed to retrieve recent reports")
                    .build();
        }
    }

    /**
     * Check if a document category represents a report
     */
    private boolean isReportCategory(String category) {
        if (category == null) return false;
        String lowerCategory = category.toLowerCase();
        return lowerCategory.contains("report") ||
               lowerCategory.contains("summary") ||
               lowerCategory.contains("visit") ||
               lowerCategory.contains("lab") ||
               lowerCategory.contains("imaging") ||
               lowerCategory.equals("clinical");
    }
}