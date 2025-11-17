package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalAllergyDto;
import com.qiaben.ciyex.dto.portal.PortalHistoryDto;
import com.qiaben.ciyex.service.portal.PortalAllergyService;
import com.qiaben.ciyex.service.portal.PortalHistoryService;
import com.qiaben.ciyex.service.VitalsService;
import com.qiaben.ciyex.dto.integration.RequestContext;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@Slf4j
public class PortalHealthController {

    private final PortalAllergyService portalAllergyService;
    private final PortalHistoryService portalHistoryService;
    private final VitalsService vitalsService;

    // 🔹 Extract patientId from Authentication. Try to get an email claim from JWT first,
    // otherwise fall back to authentication.getName(). The portal mapping expects an email.
    private Long extractPatientIdFromToken(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String authName = authentication.getName();
        String emailCandidate = null;

        try {
            // If using Jwt-based authentication the principal will be a Jwt with claims
            Object principal = authentication.getPrincipal();
            if (principal != null && principal.getClass().getName().endsWith("Jwt")) {
                // Use reflection to avoid a direct dependency on oauth2.jwt classes here
                try {
                    java.lang.reflect.Method m = principal.getClass().getMethod("getClaimAsString", String.class);
                    Object claimEmail = m.invoke(principal, "email");
                    if (claimEmail instanceof String) {
                        emailCandidate = (String) claimEmail;
                    }
                } catch (NoSuchMethodException ignore) {
                    // Not a Jwt with getClaimAsString, continue
                }
            }
        } catch (Exception ex) {
            log.debug("Could not extract email from principal: {}", ex.toString());
        }

        // Fallback to authentication name if we didn't find an email claim
        if (emailCandidate == null || emailCandidate.trim().isEmpty()) {
            emailCandidate = authName;
        }

        log.debug("Resolving EHR patient id for auth name='{}' emailCandidate='{}'", authName, emailCandidate);

        Long ehrPatientId = vitalsService.getEhrPatientIdFromPortalUserEmail(emailCandidate);
        if (ehrPatientId == null) {
            log.error("Patient record not linked to EHR for auth name='{}' emailCandidate='{}'", authName, emailCandidate);
            throw new RuntimeException("Patient record not linked to EHR");
        }
        return ehrPatientId;
    }

    private void setRequestContextOrg(HttpServletRequest request) {
        // Simplified - no org context needed for single schema
        RequestContext ctx = new RequestContext();
        ctx.setAuthToken(request.getHeader("Authorization") != null ?
            request.getHeader("Authorization").substring(7) : null);
        RequestContext.set(ctx);
    }

    // ------------------- ALLERGIES -------------------
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/allergies")
    public ResponseEntity<ApiResponse<List<PortalAllergyDto.AllergyItem>>> getMyAllergies(Authentication authentication, HttpServletRequest request) {
        try {
            Long patientId = extractPatientIdFromToken(authentication);
            setRequestContextOrg(request);
            List<PortalAllergyDto.AllergyItem> allergies = portalAllergyService.getAllergiesByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.success("Allergies retrieved successfully", allergies));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<List<PortalAllergyDto.AllergyItem>>error("Failed to retrieve allergies"));
        }
    }

    // ------------------- MEDICAL HISTORY -------------------
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PortalHistoryDto.HistoryItem>>> getMyHistory(Authentication authentication, HttpServletRequest request) {
        try {
            Long patientId = extractPatientIdFromToken(authentication);
            setRequestContextOrg(request);
            List<PortalHistoryDto.HistoryItem> history = portalHistoryService.getHistoryByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.success("Medical history retrieved successfully", history));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<List<PortalHistoryDto.HistoryItem>>error("Failed to retrieve medical history"));
        }
    }
}