package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.service.portal.PortalPatientService;
import com.qiaben.ciyex.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final JwtTokenUtil jwtUtil;

    /**
     * Get the profile of the currently logged-in patient
     * Endpoint: GET /api/portal/patient/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PortalPatientDto>> getMyProfile(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            return ResponseEntity.status(401).body(
                ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Unauthorized - missing token")
                    .build()
            );
        }
        
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
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
            HttpServletRequest request,
            @RequestBody PortalPatientDto updated) {

        String token = resolveToken(request);
        if (token == null) {
            return ResponseEntity.status(401).body(
                ApiResponse.<PortalPatientDto>builder()
                    .success(false)
                    .message("Unauthorized - missing token")
                    .build()
            );
        }
        
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
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
}
//