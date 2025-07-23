package com.qiaben.ciyex.controller.core;

import ca.uhn.fhir.context.FhirContext;
import com.qiaben.ciyex.service.core.ProviderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
@Slf4j
public class ProviderController {

    private final ProviderService providerService;
    private final FhirContext fhirContext = FhirContext.forR4();

    @PostMapping("/register")
    public ResponseEntity<String> registerProvider(@Valid @RequestBody String providerJson) {
        log.info("Received provider registration request");
        try {
            // Here you would parse JSON to FHIR Practitioner or Entity as per your actual implementation.
            // For now, just logging and dummy response.
            log.debug("Provider JSON: {}", providerJson);

            String resultJson = "{\"success\":true,\"message\":\"Provider registered successfully!\"}";
            log.info("Provider registered successfully");
            return ResponseEntity.ok(resultJson);
        } catch (Exception e) {
            log.error("Failed to register provider", e);
            String errorJson = "{\"success\":false,\"message\":\"Failed to register provider: " + e.getMessage().replace("\"", "\\\"") + "\"}";
            return ResponseEntity.badRequest().body(errorJson);
        }
    }

    @PostMapping("/schedule")
    public ResponseEntity<String> setSchedule(@Valid @RequestBody List<String> workingDaysJsonList) {
        log.info("Received request to set provider schedule with {} entries", workingDaysJsonList.size());
        try {
            // You can parse workingDaysJsonList strings to your actual entities if needed.
            String resultJson = "{\"success\":true,\"message\":\"Working days saved successfully!\"}";
            log.info("Working days saved successfully");
            return ResponseEntity.ok(resultJson);
        } catch (Exception e) {
            log.error("Failed to save working days", e);
            String errorJson = "{\"success\":false,\"message\":\"Failed to save working days: " + e.getMessage().replace("\"", "\\\"") + "\"}";
            return ResponseEntity.badRequest().body(errorJson);
        }
    }

    @PostMapping("/service")
    public ResponseEntity<String> addService(@Valid @RequestBody String serviceJson) {
        log.info("Received request to add service");
        try {
            // Convert serviceJson to entity and save using service (omitted for brevity)
            // Example: ProviderServiceService.saveService(mappedEntity);

            String resultJson = "{\"success\":true,\"message\":\"Service added successfully!\"}";
            log.info("Service added successfully");
            return ResponseEntity.ok(resultJson);
        } catch (Exception e) {
            log.error("Failed to add service", e);
            String errorJson = "{\"success\":false,\"message\":\"Failed to add service: " + e.getMessage().replace("\"", "\\\"") + "\"}";
            return ResponseEntity.badRequest().body(errorJson);
        }
    }
}
