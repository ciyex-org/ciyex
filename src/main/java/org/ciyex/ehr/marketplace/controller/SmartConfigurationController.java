package org.ciyex.ehr.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.marketplace.service.SmartLaunchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publicly accessible SMART on FHIR configuration discovery endpoint.
 * Per HL7 spec, this must be available without authentication at:
 *   GET /api/public/.well-known/smart-configuration
 *
 * SMART apps use this to discover authorization/token endpoints.
 */
@RestController
@RequiredArgsConstructor
public class SmartConfigurationController {

    private final SmartLaunchService smartLaunchService;

    @GetMapping("/api/public/.well-known/smart-configuration")
    public ResponseEntity<Map<String, Object>> smartConfiguration() {
        return ResponseEntity.ok(smartLaunchService.getSmartConfiguration());
    }
}
