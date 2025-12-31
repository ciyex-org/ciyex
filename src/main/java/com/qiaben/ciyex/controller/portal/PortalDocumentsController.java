package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.DocumentDto;
import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.service.DocumentService;
import com.qiaben.ciyex.service.VitalsService;
import com.qiaben.ciyex.service.DocumentService.DownloadResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ApiResponse<List<DocumentDto>> getMyDocuments(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build();
        }

        try {
            // Extract email from JWT token
            String userEmail = extractEmailFromAuthentication(authentication);

            // Get EHR patient ID from portal user mapping
            Long ehrPatientId = sharedVitalsService.getEhrPatientIdFromPortalUserEmail(userEmail);

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

    /**
     * Download a specific document for the currently logged-in portal patient
     * Endpoint: GET /api/fhir/portal/documents/{documentId}/download
     */
    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable String documentId,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        try {
            // Extract email from JWT token
            String userEmail = extractEmailFromAuthentication(authentication);

            // Get EHR patient ID from portal user mapping
            Long ehrPatientId = sharedVitalsService.getEhrPatientIdFromPortalUserEmail(userEmail);

            if (ehrPatientId == null) {
                return ResponseEntity.status(403).build();
            }

            // Verify the document belongs to this patient
            com.qiaben.ciyex.dto.ApiResponse<List<DocumentDto>> documentsResponse = sharedDocumentService.getAllForPatient(ehrPatientId);
            if (!documentsResponse.isSuccess() || documentsResponse.getData() == null) {
                return ResponseEntity.status(404).build();
            }

            boolean documentBelongsToPatient = documentsResponse.getData().stream()
                    .anyMatch(doc -> doc.getId().equals(documentId));

            if (!documentBelongsToPatient) {
                return ResponseEntity.status(403).build();
            }

            // Download the document
            DownloadResult result = sharedDocumentService.download(documentId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + result.getFileName() + "\"")
                    .body(new InputStreamResource(result.getInputStream()));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Download failed for document {}: {}", documentId, e.getMessage());
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            log.error("Unexpected error downloading document {}", documentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Extract email from JWT token authentication
     */
    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
            org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) authentication.getPrincipal();
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
