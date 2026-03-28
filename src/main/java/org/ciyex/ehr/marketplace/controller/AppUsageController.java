package org.ciyex.ehr.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.marketplace.dto.AppUsageSummary;
import org.ciyex.ehr.marketplace.dto.RecordUsageRequest;
import org.ciyex.ehr.marketplace.service.AppUsageService;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

/**
 * App usage metering API.
 *
 * Tracks app usage events for billing and analytics.
 * Practice admins can view usage summaries and trends.
 */
@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/app-usage")
@RequiredArgsConstructor
public class AppUsageController {

    private final AppUsageService usageService;

    /**
     * Record a usage event (called by EHR-UI when an app is used).
     */
    @PostMapping("/events")
    public ResponseEntity<Void> recordEvent(@Valid @RequestBody RecordUsageRequest request) {
        String orgId = RequestContext.get().getOrgName();
        usageService.recordEvent(orgId, request);
        return ResponseEntity.accepted().build();
    }

    /**
     * Get usage summary for all apps in the current org.
     */
    @GetMapping("/summary")
    public ResponseEntity<List<AppUsageSummary>> getUsageSummary(
            @RequestParam(defaultValue = "30") int days) {
        String orgId = RequestContext.get().getOrgName();
        return ResponseEntity.ok(usageService.getUsageSummary(orgId, days));
    }

    /**
     * Get daily usage trend for a specific app.
     */
    @GetMapping("/trend/{appSlug}")
    public ResponseEntity<List<Map<String, Object>>> getDailyTrend(
            @PathVariable String appSlug,
            @RequestParam(defaultValue = "30") int days) {
        String orgId = RequestContext.get().getOrgName();
        return ResponseEntity.ok(usageService.getDailyTrend(orgId, appSlug, days));
    }

    /**
     * Get total event count for an app.
     */
    @GetMapping("/count/{appSlug}")
    public ResponseEntity<Map<String, Object>> getEventCount(
            @PathVariable String appSlug,
            @RequestParam(defaultValue = "30") int days) {
        String orgId = RequestContext.get().getOrgName();
        long count = usageService.getEventCount(orgId, appSlug, days);
        return ResponseEntity.ok(Map.of("appSlug", appSlug, "days", days, "totalEvents", count));
    }

    /**
     * Trigger daily aggregation (admin/cron endpoint).
     */
    @PostMapping("/aggregate")
    public ResponseEntity<Map<String, Object>> triggerAggregation() {
        int aggregated = usageService.aggregateDaily();
        return ResponseEntity.ok(Map.of("aggregated", aggregated));
    }
}
