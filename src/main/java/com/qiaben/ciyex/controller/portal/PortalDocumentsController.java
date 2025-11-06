package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.service.DocumentService;
import com.qiaben.ciyex.service.VitalsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for portal patients to view their own documents
 */
@RestController
@RequestMapping("/api/fhir/portal/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalDocumentsController {

    private final DocumentService sharedDocumentService;
    private final VitalsService sharedVitalsService;

    /**
     * Get all documents for the currently logged-in portal patient
     * Endpoint: GET /api/fhir/portal/documents/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<DocumentDto>> getMyDocuments(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build();
        }

        try {
            // Get Keycloak user UUID from authentication
            String keycloakUserId = authentication.getName();

            // Get EHR patient ID from portal user mapping
            Long ehrPatientId = sharedVitalsService.getEhrPatientIdFromPortalUserEmail(keycloakUserId);

            if (ehrPatientId == null) {
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .data(null)
                        .build();
            }

            // Get all documents for this patient
            com.qiaben.ciyex.dto.ApiResponse<List<DocumentDto>> documentsResponse = sharedDocumentService.getAllForPatient(ehrPatientId);

            if (documentsResponse.isSuccess() && documentsResponse.getData() != null) {
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(true)
                        .message("Patient documents retrieved")
                        .data(documentsResponse.getData())
                        .build();
            } else {
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(false)
                        .message(documentsResponse.getMessage() != null ? documentsResponse.getMessage() : "No documents found")
                        .data(null)
                        .build();
            }

        } catch (Exception e) {
            log.error("Error retrieving documents for portal user: {}", e.getMessage(), e);
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Failed to retrieve documents: " + e.getMessage())
                    .build();
        }
    }
}