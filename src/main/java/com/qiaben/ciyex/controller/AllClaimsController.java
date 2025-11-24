package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.*;
import com.qiaben.ciyex.service.PatientBillingService;
import com.qiaben.ciyex.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/all-claims")
public class AllClaimsController {
    private final PatientBillingService service;
    private final PatientService patientService;

    @GetMapping
    public ResponseEntity<List<PatientClaimDto>> listAllClaims() {
        var data = service.listAllClaims();
        return ResponseEntity.ok(data);
    }

    /**
     * Change claim status (accepts JSON body, not request parameter)
     */
    @PutMapping("/{claimId}/status")
    public ResponseEntity<ApiResponse<PatientClaimDto>> changeClaimStatus(
            @PathVariable Long claimId,
            @RequestBody ClaimStatusUpdateDto dto
    ) {
        PatientClaimDto response = updateClaimStatus(claimId, dto);
        return ResponseEntity.ok(ApiResponse.ok("Claim status updated", response));
    }

    /**
     * Helper method to update claim status and return the updated claim DTO
     */
    private PatientClaimDto updateClaimStatus(Long claimId, ClaimStatusUpdateDto dto) {
        service.changeClaimStatus(null, claimId, dto);
        return service.getClaimDtoById(claimId);
    }

    /**
     * Void and recreate claim - POST /api/all-claims/{claimId}/void-recreate
     * Deletes the existing claim from database and creates a new DRAFT claim
     */
    @PostMapping("/{claimId}/void-recreate")
    public ResponseEntity<ApiResponse<PatientClaimDto>> voidAndRecreateClaim(
            @PathVariable Long claimId) {
        log.info("Voiding and recreating claim ID: {}", claimId);
        PatientClaimDto data = service.voidAndRecreateClaimById(claimId);
        return ResponseEntity.ok(ApiResponse.ok("Claim voided and recreated successfully", data));
    }

    /**
     * Convert claim type (manual/electronic)
     * PUT /api/all-claims/{claimId}/convert-type
     */
    @PutMapping("/{claimId}/convert-type")
    public ResponseEntity<ApiResponse<PatientClaimDto>> convertClaimType(
            @PathVariable Long claimId,
            @RequestBody ClaimTypeConvertDto dto
    ) {
        PatientClaimDto updated = service.convertClaimType(claimId, dto.getTargetType());
        return ResponseEntity.ok(ApiResponse.ok("Claim type updated", updated));
    }

    /**
     * Search patients by query string (name, MRN, etc.)
     * GET /api/all-claims/patient-search?query=John&page=0&size=20
     */
    @GetMapping("/patient-search")
    public ResponseEntity<ApiResponse<Page<PatientDto>>> searchPatients(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PatientDto> result = patientService.searchPatients(query, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("Patient search results", result));
    }

    /**
     * Get all claims for a specific patient
     * GET /api/all-claims/patient/{patientId}/claims
     */
    @GetMapping("/patient/{patientId}/claims")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> getClaimsByPatientId(@PathVariable Long patientId) {
        List<PatientClaimDto> claims = service.listAllClaimsForPatient(patientId);
        return ResponseEntity.ok(ApiResponse.ok("Claims for patient", claims));
    }

    /**
     * Get claim line details (DOS, code, description, provider, total submitted amount)
     * GET /api/all-claims/{claimId}/line-details
     */
    @GetMapping("/{claimId}/line-details")
    public ResponseEntity<ApiResponse<List<ClaimLineDetailDto>>> getClaimLineDetails(@PathVariable Long claimId) {
        List<ClaimLineDetailDto> lineDetails = service.getClaimLineDetails(claimId);
        return ResponseEntity.ok(ApiResponse.ok("Claim line details", lineDetails));
    }

    /**
     * Send claim details to insurance email
     * POST /api/all-claims/{claimId}/sends
     */
    @PostMapping("/{claimId}/sends")
    public ResponseEntity<ApiResponse<String>> sendClaimToInsurance(@PathVariable Long claimId) {
        try {
            // Fetch claim details
            PatientClaimDto claim = service.getClaimDtoById(claimId);
            if (claim == null) {
                return ResponseEntity.ok(ApiResponse.error("Claim not found"));
            }

            // Try to get insurance email from service
            String insuranceContact = service.getInsuranceEmailForClaim(claimId);


            // If email not available, build a fallback contact summary from claim/coverage fields
            String contactToUse = null;
            if (insuranceContact != null && !insuranceContact.isEmpty()) {
                contactToUse = insuranceContact;
            } else {
                StringBuilder sb = new StringBuilder();
                if (claim.planName() != null && !claim.planName().isEmpty()) {
                    sb.append("Plan: ").append(claim.planName()).append("; ");
                }
                if (claim.provider() != null && !claim.provider().isEmpty()) {
                    sb.append("Provider: ").append(claim.provider()).append("; ");
                }
                if (claim.policyNumber() != null && !claim.policyNumber().isEmpty()) {
                    sb.append("Policy: ").append(claim.policyNumber()).append("; ");
                }
                if (claim.patientName() != null && !claim.patientName().isEmpty()) {
                    sb.append("Subscriber: ").append(claim.patientName()).append("; ");
                }

                contactToUse = sb.length() > 0 ? sb.toString().trim() : null;
            }

            if (contactToUse == null || contactToUse.isEmpty()) {
                // No usable contact information found — return informative message instead of failing silently
                return ResponseEntity.ok(ApiResponse.error("No insurance contact information available for this claim"));
            }

            // Send claim details using the existing service stub. The service currently logs the contact; it can accept a contact string
            boolean sent = service.sendClaimDetailsToInsuranceEmail(claim, contactToUse);
            if (sent) {
                String msg = "Claim sent to insurance contact successfully" + (insuranceContact == null ? " (using fallback contact fields)" : "");
                return ResponseEntity.ok(ApiResponse.ok(msg, contactToUse));
            } else {
                return ResponseEntity.ok(ApiResponse.error("Failed to send claim to insurance contact"));
            }
        } catch (Exception e) {
            log.error("Error sending claim to insurance: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error("Error sending claim: " + e.getMessage()));
        }
    }
}
