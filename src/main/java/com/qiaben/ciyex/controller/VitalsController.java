

package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.VitalsDto;
import com.qiaben.ciyex.service.VitalsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/vitals", "/api/fhir/vitals"})
@RequiredArgsConstructor
@Slf4j
public class VitalsController {
    private final VitalsService service;

    @PostMapping("/{patientId}/{encounterId}")
    public ApiResponse<VitalsDto> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody VitalsDto dto) {
        return ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals recorded")
                .data(service.create(patientId, encounterId, dto))
                .build();
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ApiResponse<VitalsDto> get(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        return ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals retrieved")
                .data(service.get(patientId, encounterId, id))
                .build();
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ApiResponse<List<VitalsDto>> getByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        return ApiResponse.<List<VitalsDto>>builder()
                .success(true)
                .message("Vitals by encounter")
                .data(service.getByEncounter(patientId, encounterId))
                .build();
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ApiResponse<VitalsDto> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody VitalsDto dto) {
        return ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals updated")
                .data(service.update(patientId, encounterId, id, dto))
                .build();
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
    public ResponseEntity<byte[]> print(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        byte[] pdf = service.print(patientId, encounterId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=vitals-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // 🏥 EHR Endpoint - Staff can query any patient's vitals
    @GetMapping("/by-patient/{patientId}")
    @PreAuthorize("hasRole('PRACTITIONER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<VitalsDto>>> getVitalsForEhr(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId) {
        List<VitalsDto> vitals = service.getVitalsByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<VitalsDto>>builder()
                .success(true)
                .message("Vitals retrieved for EHR")
                .data(vitals)
                .build());
    }

    // 👩‍⚕️ Patient Portal Endpoint - Only logged-in patient can see their vitals
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<VitalsDto>> getMyVitals(HttpServletRequest request) {
        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ApiResponse.<List<VitalsDto>>builder()
                        .success(false)
                        .message("Authorization token missing")
                        .data(null)
                        .build();
            }
            
            String token = authHeader.substring(7);
            
            // Use the new tenant-aware method to get vitals
            List<VitalsDto> vitals = service.getVitalsForPortalUser(token);
            
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
    @PreAuthorize("hasRole('PRACTITIONER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VitalsDto>> addVitalsForEhr(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId,
            @RequestParam Long encounterId,
            @RequestBody VitalsDto dto) {
        VitalsDto saved = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals recorded for EHR")
                .data(saved)
                .build());
    }
}
