package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PatientEducationAssignmentDto;
import com.qiaben.ciyex.service.portal.PortalPatientEducationAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/portal/patient-education-assignments")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalPatientEducationAssignmentController {

    private final PortalPatientEducationAssignmentService service;

    /**
     * Get patient education assignments for the logged-in portal user
     * Automatically resolves patient ID from JWT email claim
     * GET /api/portal/patient-education-assignments/my-assignments
     */
    @GetMapping("/my-assignments")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<PatientEducationAssignmentDto>>> getMyAssignments(
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated request to /my-assignments");
            return ResponseEntity.ok(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .data(Collections.emptyList())
                    .build());
        }

        try {
            String email = null;

            // Extract email from JWT claim
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                email = jwt.getClaimAsString("email");
            }

            if (email == null || email.isEmpty()) {
                log.warn("No email claim found in JWT token");
                return ResponseEntity.ok(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                        .success(false)
                        .message("Invalid token: email not found")
                        .data(Collections.emptyList())
                        .build());
            }

            log.info("Fetching patient education assignments for email: {}", email);
            List<PatientEducationAssignmentDto> assignments = service.getAssignmentsByEmail(email);
            
            return ResponseEntity.ok(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                    .success(true)
                    .message("Assignments retrieved successfully")
                    .data(assignments)
                    .build());

        } catch (Exception e) {
            log.error("Error retrieving patient education assignments", e);
            return ResponseEntity.ok(ApiResponse.<List<PatientEducationAssignmentDto>>builder()
                    .success(false)
                    .message("Failed to retrieve assignments: " + e.getMessage())
                    .data(Collections.emptyList())
                    .build());
        }
    }
}
