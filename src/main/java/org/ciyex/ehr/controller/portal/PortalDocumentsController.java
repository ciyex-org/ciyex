package org.ciyex.ehr.controller.portal;

import org.ciyex.ehr.dto.DocumentDto;
import org.ciyex.ehr.dto.portal.ApiResponse;
import org.ciyex.ehr.fhir.FhirClientService;
import org.ciyex.ehr.service.DocumentService;
import org.ciyex.ehr.service.DocumentService.DownloadResult;
import org.ciyex.ehr.service.PracticeContextService;
import org.ciyex.ehr.service.portal.PortalGenericResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for portal patients to view their own documents.
 * Uses FhirClientService directly for email→patient lookup.
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
    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final PortalGenericResourceService portalResourceService;

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT') or hasAuthority('SCOPE_patient/Patient.read')")
    public ApiResponse<List<DocumentDto>> getMyDocuments(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false).message("Unauthorized - not authenticated").build();
        }

        try {
            String userEmail = extractEmailFromAuthentication(authentication);
            String ehrPatientId;
            try {
                ehrPatientId = portalResourceService.resolvePatientId(userEmail, extractJwt(authentication));
            } catch (Exception e) {
                log.debug("Could not resolve patient for portal user {}: {}", userEmail, e.getMessage());
                ehrPatientId = getEhrPatientIdFromEmail(userEmail);
            }

            if (ehrPatientId == null) {
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(false).message("Patient record not linked to EHR").data(null).build();
            }

            Long patientId = Long.parseLong(ehrPatientId);
            org.ciyex.ehr.dto.ApiResponse<List<DocumentDto>> documentsResponse =
                    sharedDocumentService.getAllForPatient(patientId);

            if (documentsResponse.isSuccess() && documentsResponse.getData() != null) {
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(true).message("Patient documents retrieved").data(documentsResponse.getData()).build();
            } else {
                return ApiResponse.<List<DocumentDto>>builder()
                        .success(false)
                        .message(documentsResponse.getMessage() != null ? documentsResponse.getMessage() : "No documents found")
                        .data(null).build();
            }
        } catch (Exception e) {
            log.error("Error retrieving documents for portal user: {}", e.getMessage(), e);
            return ApiResponse.<List<DocumentDto>>builder()
                    .success(false).message("Failed to retrieve documents: " + e.getMessage()).build();
        }
    }

    @GetMapping("/{documentId}/download")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT') or hasAuthority('SCOPE_patient/Patient.read')")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable String documentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        try {
            String userEmail = extractEmailFromAuthentication(authentication);
            String ehrPatientId;
            try {
                ehrPatientId = portalResourceService.resolvePatientId(userEmail, extractJwt(authentication));
            } catch (Exception e) {
                ehrPatientId = getEhrPatientIdFromEmail(userEmail);
            }

            if (ehrPatientId == null) {
                return ResponseEntity.status(403).build();
            }

            Long patientId = Long.parseLong(ehrPatientId);
            org.ciyex.ehr.dto.ApiResponse<List<DocumentDto>> documentsResponse =
                    sharedDocumentService.getAllForPatient(patientId);
            if (!documentsResponse.isSuccess() || documentsResponse.getData() == null) {
                return ResponseEntity.status(404).build();
            }

            // Find the matching document and verify ownership
            DocumentDto matchedDoc = documentsResponse.getData().stream()
                    .filter(doc -> doc.getId() != null && (
                        String.valueOf(doc.getId()).equals(documentId) ||
                        (doc.getFhirId() != null && doc.getFhirId().equals(documentId))
                    ))
                    .findFirst()
                    .orElse(null);
            if (matchedDoc == null) {
                return ResponseEntity.status(403).build();
            }

            // Use fhirId for download since DocumentService.download expects FHIR resource ID
            String fhirId = matchedDoc.getFhirId() != null ? matchedDoc.getFhirId() : documentId;
            DownloadResult result = sharedDocumentService.download(fhirId);
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

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT') or hasAuthority('SCOPE_patient/Patient.read')")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable String documentId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        try {
            String userEmail = extractEmailFromAuthentication(authentication);
            String ehrPatientId;
            try {
                ehrPatientId = portalResourceService.resolvePatientId(userEmail, extractJwt(authentication));
            } catch (Exception e) {
                ehrPatientId = getEhrPatientIdFromEmail(userEmail);
            }

            if (ehrPatientId == null) {
                return ResponseEntity.status(403).build();
            }

            Long patientId = Long.parseLong(ehrPatientId);
            org.ciyex.ehr.dto.ApiResponse<List<DocumentDto>> documentsResponse =
                    sharedDocumentService.getAllForPatient(patientId);
            if (!documentsResponse.isSuccess() || documentsResponse.getData() == null) {
                return ResponseEntity.status(404).build();
            }

            // Find the matching document and verify ownership
            DocumentDto matchedDoc = documentsResponse.getData().stream()
                    .filter(doc -> doc.getId() != null && (
                        String.valueOf(doc.getId()).equals(documentId) ||
                        (doc.getFhirId() != null && doc.getFhirId().equals(documentId))
                    ))
                    .findFirst()
                    .orElse(null);
            if (matchedDoc == null) {
                return ResponseEntity.status(403).build();
            }

            // Use fhirId for delete since DocumentService expects FHIR resource ID
            String fhirId = matchedDoc.getFhirId() != null ? matchedDoc.getFhirId() : documentId;
            sharedDocumentService.delete(fhirId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Document deleted").build());
        } catch (RuntimeException e) {
            log.error("Delete failed for document {}: {}", documentId, e.getMessage());
            return ResponseEntity.status(404).build();
        } catch (Exception e) {
            log.error("Unexpected error deleting document {}", documentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * FHIR Patient search by email → returns patient FHIR ID.
     */
    private String getEhrPatientIdFromEmail(String email) {
        if (email == null || email.isBlank()) return null;
        try {
            String orgAlias = practiceContextService.getPracticeId();
            Bundle bundle = fhirClientService.getClient(orgAlias).search()
                    .forResource(Patient.class)
                    .where(Patient.EMAIL.exactly().code(email))
                    .returnBundle(Bundle.class)
                    .execute();

            List<Patient> patients = fhirClientService.extractResources(bundle, Patient.class);
            if (!patients.isEmpty()) {
                return patients.get(0).getIdElement().getIdPart();
            }
        } catch (Exception e) {
            log.debug("Could not find EHR patient for portal email: {}", email, e);
        }
        return null;
    }

    private Jwt extractJwt(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }

    private String extractEmailFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) return email;
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) return preferredUsername;
        }
        return authentication.getName();
    }
}
