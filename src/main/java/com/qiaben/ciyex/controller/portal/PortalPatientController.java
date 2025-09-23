package com.qiaben.ciyex.controller.portal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qiaben.ciyex.dto.portal.PortalPatientDto;
import com.qiaben.ciyex.service.portal.PortalPatientService;
import com.qiaben.ciyex.util.JwtTokenUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/portal/patients")
public class PortalPatientController {

    private final PortalPatientService patientService;
    private final JwtTokenUtil jwtUtil; // ✅ using main JwtTokenUtil

    public PortalPatientController(PortalPatientService patientService, JwtTokenUtil jwtUtil) {
        this.patientService = patientService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Get the profile of the currently logged-in patient
     * Endpoint: GET /api/portal/patients/me
     */
    @GetMapping("/me")
    public ResponseEntity<PortalPatientDto> getMyProfile(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        PortalPatientDto dto = patientService.getByUserId(userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Update the profile of the currently logged-in patient
     * Endpoint: PUT /api/portal/patients/me
     */
    @PutMapping("/me")
    public ResponseEntity<PortalPatientDto> updateMyProfile(
            HttpServletRequest request,
            @RequestBody PortalPatientDto updated) {

        String token = resolveToken(request);
        if (token == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        PortalPatientDto savedDto = patientService.updatePatient(userId, updated);
        return ResponseEntity.ok(savedDto);
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
