package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.service.portal.PortalPatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for portal patients to view their own information
 */
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
     * Get the profile of the currently logged-in patient
     * Endpoint: GET /api/portal/patient/me
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
            // Get Keycloak user UUID from authentication
            String keycloakUserId = authentication.getName();
            return patientService.getPatientInfo(keycloakUserId);
        } catch (Exception e) {
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Failed to retrieve patient info: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Update the profile of the currently logged-in patient
     * Endpoint: PUT /api/portal/patient/me
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
            // Get Keycloak user UUID from authentication
            String keycloakUserId = authentication.getName();
            return patientService.updatePatientInfo(keycloakUserId, updated);
        } catch (Exception e) {
            return ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Failed to update patient info: " + e.getMessage())
                    .build();
        }
    }

}
//