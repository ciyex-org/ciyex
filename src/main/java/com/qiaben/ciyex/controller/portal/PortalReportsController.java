package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.service.portal.PortalReportsService;
import com.qiaben.ciyex.service.DocumentService;
import com.qiaben.ciyex.service.VitalsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for portal patients to view their own reports
 */
@RestController
@RequestMapping("/api/fhir/portal/reports")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalReportsController {

    private final PortalReportsService reportsService;
    private final DocumentService sharedDocumentService;
    private final VitalsService sharedVitalsService; // For getting EHR patient ID mapping

    /**
     * Get recent reports for the currently logged-in patient
     * Endpoint: GET /api/portal/reports/recent
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<DocumentDto>> getRecentReports(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build();
        }

        try {
            // Extract email from JWT token
            String userEmail = extractEmailFromAuthentication(authentication);
            return reportsService.getRecentReports(userEmail);
        } catch (Exception e) {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Failed to retrieve reports: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get all reports for the currently logged-in patient
     * Endpoint: GET /api/portal/reports
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<DocumentDto>> getAllReports(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build();
        }

        try {
            // Extract email from JWT token
            String userEmail = extractEmailFromAuthentication(authentication);
            return reportsService.getAllReports(userEmail);
        } catch (Exception e) {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Failed to retrieve reports: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Extract Bearer token from Authorization header
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Get reports for the currently logged-in portal patient (same as /api/reports/my but through portal proxy)
     * Endpoint: GET /api/portal/reports/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<DocumentDto>> getMyReports(
            Authentication authentication) {

        // Extract email from JWT token
        String userEmail = extractEmailFromAuthentication(authentication);
        Long ehrPatientId = sharedVitalsService.getEhrPatientIdFromPortalUserEmail(userEmail);

        if (ehrPatientId == null) {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Patient record not linked to EHR")
                    .data(null)
                    .build();
        }

        // Get all documents for this patient and filter for reports
        com.qiaben.ciyex.dto.ApiResponse<List<DocumentDto>> documentsResponse = sharedDocumentService.getAllForPatient(ehrPatientId);
        if (documentsResponse.isSuccess() && documentsResponse.getData() != null) {
            List<DocumentDto> reports = documentsResponse.getData()
                    .stream()
                    .filter(doc -> isReportCategory(doc.getCategory()))
                    .collect(java.util.stream.Collectors.toList());

            return ApiResponse.<List<DocumentDto>>builder()
                    .success(true)
                    .message("Patient reports retrieved")
                    .data(reports)
                    .build();
        } else {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(true)
                    .message(documentsResponse.getMessage() != null ? documentsResponse.getMessage() : "No reports found")
                    .data(new java.util.ArrayList<>())
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

    /**
     * Extract email from JWT token authentication
     */
    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
        }
        return authentication.getName(); // fallback
    }
}