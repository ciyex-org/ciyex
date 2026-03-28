package org.ciyex.ehr.marketplace.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.marketplace.dto.LaunchRequest;
import org.ciyex.ehr.marketplace.dto.SmartLaunchRequest;
import org.ciyex.ehr.marketplace.service.AppInstallationService;
import org.ciyex.ehr.marketplace.service.AppUsageService;
import org.ciyex.ehr.marketplace.service.SmartLaunchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;
import java.util.UUID;

/**
 * Handles SMART on FHIR app launch flow.
 *
 * When a user launches an app from within the EHR:
 * 1. Frontend calls POST /api/smart-launch/{appSlug} with patient/encounter context
 * 2. This controller generates a signed launch context token
 * 3. Returns the launch URL with context parameters
 * 4. Frontend redirects to the launch URL (iframe or new tab)
 *
 * The launched app then exchanges the launch token at the FHIR token endpoint
 * using standard SMART on FHIR authorization.
 */
@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/smart-launch")
@RequiredArgsConstructor
@Slf4j
public class SmartLaunchController {

    private final SmartLaunchService smartLaunchService;
    private final AppInstallationService installationService;
    private final AppUsageService usageService;

    /**
     * Generate a SMART launch context for an installed app.
     * Returns a launch URL with signed context parameters.
     */
    @PostMapping("/{appSlug}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> launchApp(
            @PathVariable String appSlug,
            @RequestBody SmartLaunchRequest request,
            Authentication authentication) {

        String orgId = RequestContext.get().getOrgName();
        String userId = authentication != null ? authentication.getName() : "anonymous";

        // Verify app is installed and active
        if (!installationService.isInstalled(orgId, appSlug)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.ok("App not installed or inactive", null));
        }

        // Generate launch context
        Map<String, Object> launchContext = smartLaunchService.createLaunchContext(
                orgId, appSlug, userId, request);

        // Log the launch for HIPAA audit
        UUID patientUuid = request.getPatientId() != null ? UUID.fromString(request.getPatientId()) : null;
        UUID encounterUuid = request.getEncounterId() != null ? UUID.fromString(request.getEncounterId()) : null;
        LaunchRequest auditRequest = LaunchRequest.builder()
                .patientId(patientUuid)
                .encounterId(encounterUuid)
                .launchType("smart")
                .build();
        installationService.logLaunch(orgId, appSlug, userId, auditRequest);

        // Record usage event for metering
        usageService.recordEvent(orgId, appSlug, "smart_launch", null, userId, patientUuid, encounterUuid);

        log.info("SMART launch for app={} org={} user={} patient={}",
                appSlug, orgId, userId, request.getPatientId());
        return ResponseEntity.ok(ApiResponse.ok("Launch context created", launchContext));
    }

    /**
     * Retrieve SMART configuration metadata (.well-known/smart-configuration).
     * Apps use this to discover the authorization/token endpoints.
     */
    @GetMapping("/metadata")
    public ResponseEntity<Map<String, Object>> smartConfiguration() {
        return ResponseEntity.ok(smartLaunchService.getSmartConfiguration());
    }
}
