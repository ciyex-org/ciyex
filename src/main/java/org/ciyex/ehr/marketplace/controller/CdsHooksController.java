package org.ciyex.ehr.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.marketplace.dto.CdsHookRequest;
import org.ciyex.ehr.marketplace.dto.CdsHookResponse;
import org.ciyex.ehr.marketplace.dto.CdsServiceDescriptor;
import org.ciyex.ehr.marketplace.service.AppUsageService;
import org.ciyex.ehr.marketplace.service.CdsHooksService;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

/**
 * CDS Hooks API endpoints per HL7 CDS Hooks specification.
 *
 * Provides discovery and invocation of CDS services from installed apps.
 * All endpoints are authenticated (user must be logged in with an org context).
 *
 * @see <a href="https://cds-hooks.hl7.org/">CDS Hooks Specification</a>
 */
@PreAuthorize("hasAuthority('SCOPE_user/Observation.read')")
@RestController
@RequestMapping("/api/cds-hooks")
@RequiredArgsConstructor
public class CdsHooksController {

    private final CdsHooksService cdsHooksService;
    private final AppUsageService usageService;

    /**
     * CDS Hooks Discovery — returns all CDS services available for the current org.
     * This aggregates services from all installed apps that support CDS Hooks.
     *
     * Per spec: GET /cds-services
     */
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> discoverServices() {
        String orgId = RequestContext.get().getOrgName();
        List<CdsServiceDescriptor> services = cdsHooksService.discoverServices(orgId);
        return ResponseEntity.ok(Map.of("services", services));
    }

    /**
     * Discover CDS services for a specific hook type.
     * Useful for the UI to know which services will fire at a given hook point.
     */
    @GetMapping("/services/hook/{hookType}")
    public ResponseEntity<Map<String, Object>> discoverServicesForHook(@PathVariable String hookType) {
        String orgId = RequestContext.get().getOrgName();
        List<CdsServiceDescriptor> services = cdsHooksService.discoverServicesForHook(orgId, hookType);
        return ResponseEntity.ok(Map.of("services", services));
    }

    /**
     * Invoke all CDS services for a given hook type.
     * Called by the EHR-UI at hook points (opening patient chart, signing orders, etc.).
     * Fans out to all matching CDS services in parallel and aggregates cards.
     *
     * Per spec: POST /cds-services/{serviceId}
     * (We aggregate across all services for the hook type)
     */
    @PostMapping("/invoke")
    public ResponseEntity<CdsHookResponse> invokeHook(@RequestBody CdsHookRequest request) {
        String orgId = RequestContext.get().getOrgName();
        CdsHookResponse response = cdsHooksService.invokeHook(orgId, request);

        // Record usage events for each CDS service that was invoked
        if (response.getServicesInvoked() > 0) {
            try {
                usageService.recordEvent(orgId, "cds-hooks", "cds_hook_invocation",
                        request.getHook(), null, null, null);
            } catch (Exception e) {
                // Don't fail the CDS response if usage tracking fails
            }
        }

        return ResponseEntity.ok(response);
    }
}
