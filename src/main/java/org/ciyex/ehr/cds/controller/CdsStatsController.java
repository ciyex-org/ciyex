package org.ciyex.ehr.cds.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.cds.service.CdsService;
import org.ciyex.ehr.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

/**
 * Exposes /api/cds/stats — combined dashboard stats used by the CDS page.
 * Returns: totalRules, activeRules, alertsToday, alerts7d, criticalAlerts, overrideRate.
 */
@PreAuthorize("hasAuthority('SCOPE_user/Observation.read')")
@RestController
@RequestMapping("/api/cds")
@RequiredArgsConstructor
@Slf4j
public class CdsStatsController {

    private final CdsService service;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        try {
            var stats = service.cdsStats();
            return ResponseEntity.ok(ApiResponse.ok("CDS stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get CDS stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
