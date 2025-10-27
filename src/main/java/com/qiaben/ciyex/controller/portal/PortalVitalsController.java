package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalVitalsDto;
import com.qiaben.ciyex.service.portal.PortalVitalsService;
import com.qiaben.ciyex.service.VitalsService;
import com.qiaben.ciyex.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for portal patients to view their own vitals
 */
@RestController
@RequestMapping("/api/portal/vitals")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalVitalsController {

    private final PortalVitalsService vitalsService;
    private final VitalsService sharedVitalsService;
    private final JwtTokenUtil jwtUtil;

    /**
     * Get the recent vitals for the currently logged-in patient
     * Endpoint: GET /api/portal/vitals/recent
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<PortalVitalsDto>> getRecentVitals(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            return ApiResponse.<List<PortalVitalsDto>>builder()
                    .success(false)
                    .message("Unauthorized - missing token")
                    .build();
        }

        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            return vitalsService.getRecentVitals(userId);
        } catch (Exception e) {
            return ApiResponse.<List<PortalVitalsDto>>builder()
                    .success(false)
                    .message("Invalid token")
                    .build();
        }
    }

    /**
     * Get all vitals for the currently logged-in patient
     * Endpoint: GET /api/portal/vitals
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<PortalVitalsDto>> getAllVitals(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            return ApiResponse.<List<PortalVitalsDto>>builder()
                    .success(false)
                    .message("Unauthorized - missing token")
                    .build();
        }

        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            return vitalsService.getAllVitals(userId);
        } catch (Exception e) {
            return ApiResponse.<List<PortalVitalsDto>>builder()
                    .success(false)
                    .message("Invalid token")
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
     * Get vitals for the currently logged-in portal patient (same as /api/vitals/my but through portal proxy)
     * Endpoint: GET /api/portal/vitals/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<PortalVitalsDto>> getMyVitals(
            @RequestHeader(value = "x-org-id", required = false) Long orgId,
            Authentication authentication) {

        String email = authentication.getName();
        Long ehrPatientId = sharedVitalsService.getEhrPatientIdFromPortalUserEmail(email, orgId);

        if (ehrPatientId == null) {
            return ApiResponse.<List<PortalVitalsDto>>builder()
                    .success(false)
                    .message("Patient record not linked to EHR")
                    .data(null)
                    .build();
        }

        // Get all vitals for this patient using the shared service
        List<PortalVitalsDto> vitals = sharedVitalsService.getVitalsByPatient(orgId, ehrPatientId)
                .stream()
                .map(vital -> PortalVitalsDto.builder()
                        .id(vital.getId())
                        .patientId(vital.getPatientId())
                        .encounterId(vital.getEncounterId())
                        .weightKg(vital.getWeightKg())
                        .weightLbs(vital.getWeightLbs())
                        .bpSystolic(vital.getBpSystolic())
                        .bpDiastolic(vital.getBpDiastolic())
                        .pulse(vital.getPulse())
                        .respiration(vital.getRespiration())
                        .temperatureC(vital.getTemperatureC())
                        .temperatureF(vital.getTemperatureF())
                        .oxygenSaturation(vital.getOxygenSaturation())
                        .bmi(vital.getBmi())
                        .notes(vital.getNotes())
                        .recordedAt(vital.getRecordedAt())
                        .createdDate(vital.getCreatedDate())
                        .build())
                .collect(java.util.stream.Collectors.toList());

        return ApiResponse.<List<PortalVitalsDto>>builder()
                .success(true)
                .message("Patient vitals retrieved")
                .data(vitals)
                .build();
    }
}