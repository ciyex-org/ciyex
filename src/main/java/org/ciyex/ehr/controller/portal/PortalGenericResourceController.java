package org.ciyex.ehr.controller.portal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.portal.ApiResponse;
import org.ciyex.ehr.service.portal.PortalGenericResourceService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

/**
 * Portal-scoped generic FHIR resource controller.
 *
 * Provides configurable, self-service access to FHIR data for portal users.
 * Fields are driven by tab_field_config — adding/removing/reordering fields
 * requires only a config change, no code changes.
 *
 * Endpoints:
 *   GET  /api/portal/resource/{tabKey}/config  — field config for dynamic form rendering
 *   GET  /api/portal/resource/{tabKey}          — resource data for current user
 *   PUT  /api/portal/resource/{tabKey}          — update resource data for current user
 */
@Slf4j
@PreAuthorize("hasAuthority('SCOPE_patient/Patient.read')")
@RestController
@RequestMapping("/api/portal/resource")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalGenericResourceController {

    private final PortalGenericResourceService portalResourceService;

    /**
     * Get field config for a portal tab.
     * Frontend uses this to render dynamic forms — sections, fields, validation, options.
     */
    @GetMapping("/{tabKey}/config")
    public ApiResponse<Map<String, Object>> getConfig(@PathVariable String tabKey) {
        try {
            Map<String, Object> config = portalResourceService.getFieldConfig(tabKey);
            return ApiResponse.success("Config retrieved", config);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Tab not found: " + tabKey);
        } catch (Exception e) {
            log.error("Error getting config for tab: {}", tabKey, e);
            return ApiResponse.error("Failed to get config: " + e.getMessage());
        }
    }

    /**
     * Get resource data for the current portal user.
     * Resolves user email → linked Patient → reads via GenericFhirResourceService.
     */
    @GetMapping("/{tabKey}")
    public ApiResponse<Map<String, Object>> getData(
            @PathVariable String tabKey,
            Authentication authentication) {

        String email = extractEmail(authentication);
        if (email == null) {
            return ApiResponse.error("Invalid token: email not found");
        }

        try {
            Map<String, Object> data = portalResourceService.getData(tabKey, email, extractJwt(authentication));
            return ApiResponse.success("Data retrieved", data);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // User not found, not approved, or not linked
            return ApiResponse.error("Portal account not linked to a medical record. Please contact your provider.");
        } catch (Exception e) {
            // FHIR search failure (e.g., Person partition not available) — treat as not linked
            String msg = e.getMessage();
            if (msg != null && (msg.contains("Person") || msg.contains("not found") || msg.contains("Bad Request"))) {
                return ApiResponse.error("Portal account not linked to a medical record. Please contact your provider.");
            }
            log.error("Error getting data for tab: {} email: {}", tabKey, email, e);
            return ApiResponse.error("Failed to get data: " + msg);
        }
    }

    /**
     * Update resource data for the current portal user.
     * Resolves user → linked Patient → updates via GenericFhirResourceService.
     * Detects changes and posts notification to messaging channels.
     */
    @PutMapping("/{tabKey}")
    public ApiResponse<Map<String, Object>> updateData(
            @PathVariable String tabKey,
            @RequestBody Map<String, Object> formData,
            Authentication authentication) {

        String email = extractEmail(authentication);
        if (email == null) {
            return ApiResponse.error("Invalid token: email not found");
        }

        try {
            Map<String, Object> result = portalResourceService.updateData(tabKey, email, formData, extractJwt(authentication));
            return ApiResponse.success("Updated successfully", result);
        } catch (IllegalStateException e) {
            return ApiResponse.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating data for tab: {} email: {}", tabKey, email, e);
            return ApiResponse.error("Failed to update: " + e.getMessage());
        }
    }

    private String extractEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }
        return null;
    }

    private Jwt extractJwt(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}
