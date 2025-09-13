package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.TenantSchemaInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final TenantSchemaInitializer tenantSchemaInitializer;

    @PostMapping("/tenant-schema")
    public ResponseEntity<Map<String, String>> createTenantSchema(@RequestBody Map<String, Object> request) {
        try {
            Long orgId = Long.valueOf(request.get("orgId").toString());
            log.info("Creating tenant schema for orgId: {}", orgId);
            
            tenantSchemaInitializer.initializeTenantSchema(orgId);
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Tenant schema created successfully for orgId: " + orgId,
                "schemaName", "practice_" + orgId
            ));
        } catch (Exception e) {
            log.error("Failed to create tenant schema", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to create tenant schema: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/schemas")
    public ResponseEntity<Map<String, Object>> listSchemas() {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Use database client to check schemas: SELECT schema_name FROM information_schema.schemata;"
        ));
    }
    
    @PostMapping("/enhanced-entity-scan")
    public ResponseEntity<Map<String, String>> testEnhancedEntityScanning() {
        try {
            log.info("Testing enhanced entity scanning via API");
            tenantSchemaInitializer.testEnhancedEntityScanning();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Enhanced entity scanning test completed - check logs for details"
            ));
        } catch (Exception e) {
            log.error("Enhanced entity scanning test failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Enhanced entity scanning test failed: " + e.getMessage()
            ));
        }
    }
}
