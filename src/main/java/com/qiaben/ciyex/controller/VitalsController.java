package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.VitalsDto;
import com.qiaben.ciyex.service.VitalsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/vitals", "/api/fhir/vitals"})
@RequiredArgsConstructor
@Slf4j
public class VitalsController {
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<VitalsDto>>> getAllByPatient(@PathVariable Long patientId) {
        try {
            var items = service.getAllByPatient(patientId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<VitalsDto>>builder()
                        .success(true)
                        .message("No Vitals found for Patient ID: " + patientId)
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<VitalsDto>>builder()
                    .success(true)
                    .message("Vitals fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Vitals for Patient ID: " + patientId, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<VitalsDto>>builder()
                            .success(false)
                            .message("Error fetching Vitals for Patient ID: " + patientId + ". " + ex.getMessage())
                            .build());
        }
    }
    private final VitalsService service;

    // CREATE: validate mandatory vitals fields
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<VitalsDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody VitalsDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<VitalsDto>builder()
                    .success(false).message(validationError).build());
        }

        return ResponseEntity.ok(ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals recorded")
                .data(service.create(patientId, encounterId, dto))
                .build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<VitalsDto>> get(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            var dto = service.get(patientId, encounterId, id);
            return ResponseEntity.ok(ApiResponse.<VitalsDto>builder()
                    .success(true)
                    .message("Vitals retrieved successfully")
                    .data(dto)
                    .build());
        } catch (IllegalArgumentException ex) {
            log.error("Vitals not found: " + ex.getMessage());
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<VitalsDto>builder()
                            .success(false)
                            .message(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            log.error("Error fetching Vitals for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<VitalsDto>builder()
                            .success(false)
                            .message("Error fetching Vitals: " + ex.getMessage())
                            .build());
        }
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<VitalsDto>>> getByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        try {
            var items = service.getByEncounter(patientId, encounterId);
            if (items.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.<List<VitalsDto>>builder()
                        .success(true)
                        .message(String.format("No Vitals found for Patient ID: %d, Encounter ID: %d", patientId, encounterId))
                        .data(items)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<List<VitalsDto>>builder()
                    .success(true)
                    .message("Vitals by encounter fetched successfully")
                    .data(items)
                    .build());
        } catch (Exception ex) {
            log.error("Error fetching Vitals for Patient ID: " + patientId + ", Encounter ID: " + encounterId, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<VitalsDto>>builder()
                            .success(false)
                            .message(String.format("Error fetching Vitals for Patient ID: %d, Encounter ID: %d. %s", patientId, encounterId, ex.getMessage()))
                            .build());
        }
    }

    // UPDATE: validate mandatory vitals fields
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<VitalsDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody VitalsDto dto) {
        // Validate mandatory fields
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<VitalsDto>builder()
                    .success(false).message(validationError).build());
        }

        return ResponseEntity.ok(ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals updated")
                .data(service.update(patientId, encounterId, id, dto))
                .build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        service.delete(patientId, encounterId, id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Vitals deleted")
                .build();
    }

    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ApiResponse<VitalsDto> eSign(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        return ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals signed")
                .data(service.eSign(patientId, encounterId, id))
                .build();
    }

    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<?> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        try {
            byte[] pdf = service.print(patientId, encounterId, id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=vitals-" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException ex) {
            log.error("Error printing Vitals for Patient ID: " + patientId + ", Encounter ID: " + encounterId + ", ID: " + id, ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message(ex.getMessage()).build());
        } catch (Exception ex) {
            log.error("Error generating Vitals PDF", ex);
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.<Void>builder().success(false).message("Error generating PDF: " + ex.getMessage()).build());
        }
    }

    // 🏥 EHR Endpoint - Staff can query any patient's vitals
    @GetMapping("/by-patient/{patientId}")
    @PreAuthorize("hasAuthority('ROLE_PRACTITIONER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<VitalsDto>>> getVitalsForEhr(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId) {
        List<VitalsDto> vitals = service.getVitalsByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.<List<VitalsDto>>builder()
                .success(true)
                .message("Vitals retrieved for EHR")
                .data(vitals)
                .build());
    }

    // 👩‍⚕️ Patient Portal Endpoint - Only logged-in patient can see their vitals
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_PATIENT')")
    public ApiResponse<List<VitalsDto>> getMyVitals(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ApiResponse.<List<VitalsDto>>builder()
                        .success(false)
                        .message("Unauthorized - not authenticated")
                        .data(null)
                        .build();
            }

            // Prefer the JWT email claim (or preferred_username) when available; authentication.getName()
            // can return the subject (UUID) which doesn't match portal user email records.
            String tempEmail = null;
            Object principal = authentication.getPrincipal();
            if (principal instanceof Jwt) {
                Jwt jwt = (Jwt) principal;
                tempEmail = jwt.getClaimAsString("email");
                if (tempEmail == null || tempEmail.isBlank()) {
                    tempEmail = jwt.getClaimAsString("preferred_username");
                }
            }
            if (tempEmail == null || tempEmail.isBlank()) {
                tempEmail = authentication.getName();
            }
            final String userEmail = tempEmail;

            log.info("Getting vitals for authenticated user: {}", userEmail);

            // Get vitals using the service method
            List<VitalsDto> vitals = service.getVitalsForPortalUser(userEmail);

            return ApiResponse.<List<VitalsDto>>builder()
                    .success(true)
                    .message("Patient vitals retrieved")
                    .data(vitals)
                    .build();

        } catch (Exception e) {
            log.error("Error getting vitals for portal user: {}", e.getMessage(), e);
            return ApiResponse.<List<VitalsDto>>builder()
                    .success(false)
                    .message("Error retrieving vitals: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }



    // 🏥 EHR Endpoint - Staff can add vitals for any patient
    @PostMapping("/by-patient/{patientId}")
    @PreAuthorize("hasAuthority('ROLE_PRACTITIONER') or hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<VitalsDto>> addVitalsForEhr(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId,
            @RequestParam Long encounterId,
            @RequestBody VitalsDto dto) {
        // Validate mandatory fields for EHR create
        String validationError = validateMandatoryFields(dto);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(ApiResponse.<VitalsDto>builder()
                    .success(false).message(validationError).build());
        }

        VitalsDto saved = service.create(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals recorded for EHR")
                .data(saved)
                .build());
    }

    /**
     * Validates mandatory fields for Vitals creation and update
     * @param dto VitalsDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(VitalsDto dto) {
        StringBuilder missing = new StringBuilder();

        if (dto == null) {
            return "Missing request body";
        }

        // weightKg (number) - accept null or zero as missing depending on domain; require non-null
        if (dto.getWeightKg() == null) {
            missing.append("weightKg, ");
        }
        // heightCm
        if (dto.getHeightCm() == null) {
            missing.append("heightCm, ");
        }
        // pulse
        if (dto.getPulse() == null) {
            missing.append("pulse, ");
        }
        // respiration
        if (dto.getRespiration() == null) {
            missing.append("respiration, ");
        }

        if (missing.length() > 0) {
            missing.setLength(missing.length() - 2); // remove trailing comma+space
            return "Missing mandatory fields: " + missing.toString();
        }
        return null;
    }
}


