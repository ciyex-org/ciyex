package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.service.portal.PortalPatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<PortalPatientDto>> getMyProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(
                ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build()
            );
        }
        
        try {
            // Get user email from JWT token
            String email = authentication.getName();
            // TODO: Get userId from email or use email directly
            Long userId = 1L; // Placeholder - implement proper user lookup
            ApiResponse<PortalPatientDto> response = patientService.getPatientInfo(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Invalid token")
                    .build()
            );
        }
    }

    /**
     * Update the profile of the currently logged-in patient
     * Endpoint: PUT /api/portal/patient/me
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<PortalPatientDto>> updateMyProfile(
            Authentication authentication,
            @RequestBody PortalPatientDto updated) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(
                ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build()
            );
        }
        
        try {
            // Get user email from JWT token
            String email = authentication.getName();
            // TODO: Get userId from email or use email directly
            Long userId = 1L; // Placeholder - implement proper user lookup
            ApiResponse<PortalPatientDto> response = patientService.updatePatientInfo(userId, updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Invalid token")
                    .build()
            );
        }
    }
}
//