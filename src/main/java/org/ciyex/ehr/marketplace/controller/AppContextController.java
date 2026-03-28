package org.ciyex.ehr.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.marketplace.dto.AppInstallationResponse;
import org.ciyex.ehr.marketplace.service.AppInstallationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

/**
 * Returns installed apps filtered by UI context or extension point.
 *
 * Used by the EHR-UI to determine which plugins to activate on a given screen.
 * For example, the patient chart page calls GET /api/app-context/patient-chart
 * to get all apps that contribute to patient-chart:tab, patient-chart:action-bar, etc.
 */
@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/app-context")
@RequiredArgsConstructor
public class AppContextController {

    private final AppInstallationService service;

    /**
     * Get installed apps relevant to a UI context (prefix match).
     * e.g., GET /api/app-context/patient-chart returns apps with extension points
     * like "patient-chart:tab", "patient-chart:action-bar", "patient-chart:banner-alert"
     */
    @GetMapping("/{context}")
    public ResponseEntity<ApiResponse<List<AppInstallationResponse>>> getAppsForContext(
            @PathVariable String context) {
        String orgId = RequestContext.get().getOrgName();
        var apps = service.getAppsForContext(orgId, context);
        return ResponseEntity.ok(ApiResponse.ok("Apps for context: " + context, apps));
    }

    /**
     * Get installed apps for a specific extension point (exact match).
     * e.g., GET /api/app-context/slot/patient-chart:tab
     */
    @GetMapping("/slot/{slotName}")
    public ResponseEntity<ApiResponse<List<AppInstallationResponse>>> getAppsForSlot(
            @PathVariable String slotName) {
        String orgId = RequestContext.get().getOrgName();
        var apps = service.getAppsForSlot(orgId, slotName);
        return ResponseEntity.ok(ApiResponse.ok("Apps for slot: " + slotName, apps));
    }
}
