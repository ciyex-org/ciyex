package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.*;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
import org.ciyex.ehr.service.PatientClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@Slf4j
@PreAuthorize("hasAuthority('SCOPE_user/Claim.read')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/all-claims")
public class AllClaimsController {
    private final PatientClaimService service;
    private final GenericFhirResourceService fhirService;

    @GetMapping
    public ResponseEntity<List<PatientClaimDto>> listAllClaims() {
        var data = service.listAllClaims();
        return ResponseEntity.ok(data);
    }

    @RequestMapping(value = "/{claimId}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<PatientClaimDto>> changeClaimStatus(
            @PathVariable Long claimId,
            @RequestBody ClaimStatusUpdateDto dto) {
        service.changeClaimStatus(null, claimId, dto);
        PatientClaimDto response = service.getClaimDtoById(claimId);
        return ResponseEntity.ok(ApiResponse.ok("Claim status updated", response));
    }

    @RequestMapping(value = "/{claimId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<PatientClaimDto>> updateClaim(
            @PathVariable Long claimId,
            @RequestBody Map<String, Object> body) {
        try {
            PatientClaimDto existing = service.getClaimDtoById(claimId);
            if (existing == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("Claim not found: " + claimId));
            }
            var update = new PatientClaimService.PatientClaimCoreUpdate(
                    body.getOrDefault("treatingProviderId", existing.treatingProviderId()) != null
                            ? String.valueOf(body.getOrDefault("treatingProviderId", existing.treatingProviderId())) : null,
                    body.getOrDefault("billingEntity", existing.billingEntity()) != null
                            ? String.valueOf(body.getOrDefault("billingEntity", existing.billingEntity())) : null,
                    body.getOrDefault("type", existing.type()) != null
                            ? String.valueOf(body.getOrDefault("type", existing.type())) : null,
                    body.getOrDefault("notes", existing.notes()) != null
                            ? String.valueOf(body.getOrDefault("notes", existing.notes())) : null,
                    null, null, null, null
            );
            PatientClaimDto updated = service.updateClaimById(claimId, update);
            return ResponseEntity.ok(ApiResponse.ok("Claim updated", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update claim {}", claimId, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to update claim"));
        }
    }

    @PostMapping("/{claimId}/void-recreate")
    public ResponseEntity<ApiResponse<PatientClaimDto>> voidAndRecreateClaim(
            @PathVariable Long claimId) {
        log.info("Voiding and recreating claim ID: {}", claimId);
        PatientClaimDto data = service.voidAndRecreateClaimById(claimId);
        return ResponseEntity.ok(ApiResponse.ok("Claim voided and recreated successfully", data));
    }

    @PutMapping("/{claimId}/convert-type")
    public ResponseEntity<ApiResponse<PatientClaimDto>> convertClaimType(
            @PathVariable Long claimId,
            @RequestBody ClaimTypeConvertDto dto) {
        PatientClaimDto updated = service.convertClaimType(claimId, dto.getTargetType());
        return ResponseEntity.ok(ApiResponse.ok("Claim type updated", updated));
    }

    /**
     * Search patients by query string (name, MRN, etc.)
     * Returns generic FHIR data from demographics tab.
     */
    @GetMapping("/diagnosis-search")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> searchByDiagnosis(
            @RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("No query", List.of()));
        }
        String q = query.trim().toLowerCase();
        List<PatientClaimDto> all = service.listAllClaims();
        List<PatientClaimDto> filtered = all.stream()
                .filter(c -> (c.diagnosisCode() != null && c.diagnosisCode().toLowerCase().contains(q))
                        || (c.notes() != null && c.notes().toLowerCase().contains(q)))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Diagnosis search results", filtered));
    }

    @GetMapping("/patient-search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchPatients(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> result = fhirService.searchByName("demographics", query, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Patient search results", result));
    }

    @GetMapping("/patient/{patientId}/claims")
    public ResponseEntity<ApiResponse<List<PatientClaimDto>>> getClaimsByPatientId(@PathVariable Long patientId) {
        try {
            List<PatientClaimDto> claims = service.listAllClaimsForPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Claims for patient", claims));
        } catch (IllegalArgumentException e) {
            log.warn("Failed to fetch claims for patient {}: {}", patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.ok("Claims for patient", List.of()));
        } catch (Exception e) {
            log.error("Error fetching claims for patient {}", patientId, e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Failed to fetch claims"));
        }
    }

    @GetMapping("/{claimId}/line-details")
    public ResponseEntity<ApiResponse<List<ClaimLineDetailDto>>> getClaimLineDetails(@PathVariable Long claimId) {
        List<ClaimLineDetailDto> lineDetails = service.getClaimLineDetails(claimId);
        return ResponseEntity.ok(ApiResponse.ok("Claim line details", lineDetails));
    }

    @PostMapping("/{claimId}/sends")
    public ResponseEntity<ApiResponse<String>> sendClaimToInsurance(@PathVariable Long claimId) {
        try {
            PatientClaimDto claim = service.getClaimDtoById(claimId);
            if (claim == null) {
                return ResponseEntity.ok(ApiResponse.error("Claim not found"));
            }

            String insuranceContact = service.getInsuranceEmailForClaim(claimId);

            String contactToUse = null;
            if (insuranceContact != null && !insuranceContact.isEmpty()) {
                contactToUse = insuranceContact;
            } else {
                StringBuilder sb = new StringBuilder();
                if (claim.planName() != null && !claim.planName().isEmpty())
                    sb.append("Plan: ").append(claim.planName()).append("; ");
                if (claim.provider() != null && !claim.provider().isEmpty())
                    sb.append("Provider: ").append(claim.provider()).append("; ");
                if (claim.policyNumber() != null && !claim.policyNumber().isEmpty())
                    sb.append("Policy: ").append(claim.policyNumber()).append("; ");
                if (claim.patientName() != null && !claim.patientName().isEmpty())
                    sb.append("Subscriber: ").append(claim.patientName()).append("; ");
                contactToUse = sb.length() > 0 ? sb.toString().trim() : null;
            }

            if (contactToUse == null || contactToUse.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error("No insurance contact information available for this claim"));
            }

            boolean sent = service.sendClaimDetailsToInsuranceEmail(claim, contactToUse);
            if (sent) {
                String msg = "Claim sent to insurance contact successfully" +
                        (insuranceContact == null ? " (using fallback contact fields)" : "");
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
