package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.service.portal.PortalPatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/portal/patient")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalPatientController {

    private final PortalPatientService patientService;

    /**
     * Get logged-in patient profile
     * GET /api/portal/patient/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<PortalPatientDto> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build();
        }

        try {
            String email = null;

            // ✅ Extract email from JWT claim
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                email = jwt.getClaimAsString("email");
            }

            if (email == null || email.isEmpty()) {
                log.warn("No email claim found in token");
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("Invalid token: email not found")
                        .build();
            }

            log.info("Fetching portal patient for email: {}", email);
            return patientService.getPatientInfo(email);

        } catch (Exception e) {
            log.error("Error retrieving portal patient info", e);
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Failed to retrieve patient info: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Update logged-in patient profile
     * PUT /api/portal/patient/me
     */
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<PortalPatientDto> updateMyProfile(
            Authentication authentication,
            @RequestBody PortalPatientDto updated) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build();
        }

        try {
            String email = null;
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                email = jwt.getClaimAsString("email");
            }

            if (email == null || email.isEmpty()) {
                log.warn("No email claim found in token during update");
                return ApiResponse.<PortalPatientDto>builder()
                        .success(false)
                        .message("Invalid token: email not found")
                        .build();
            }

            log.info("Updating portal patient for email: {}", email);
            return patientService.updatePatientInfo(email, updated);

        } catch (Exception e) {
            log.error("Error updating portal patient info", e);
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Failed to update patient info: " + e.getMessage())
                    .build();
        }
    }
}
