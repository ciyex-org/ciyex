// OrgConfigController.java
package org.ciyex.ehr.controller;

import org.ciyex.ehr.service.OrgConfigService;
import org.ciyex.ehr.service.OrgConfigService.OrgConfigResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@Slf4j
@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orgConfig")
public class OrgConfigController {

    private final OrgConfigService orgConfigService;

    @GetMapping
    public ResponseEntity<List<OrgConfigResult>> getAllConfigs() {
        log.info("[GET] /api/orgConfig - fetching all configs");
        List<OrgConfigResult> configs = orgConfigService.getAllConfigs();
        log.info("[GET] /api/orgConfig - fetched {} rows", configs.size());
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/map")
    public ResponseEntity<Map<String, String>> getAllConfigsAsMap() {
        log.info("[GET] /api/orgConfig/map - fetching as map");
        Map<String, String> configs = orgConfigService.getAllConfigsAsMap();
        log.info("[GET] /api/orgConfig/map - fetched {} keys", configs.size());
        return ResponseEntity.ok(configs);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> postMultipleConfigs(@RequestBody Map<String, String> configs) {
        log.info("[POST] /api/orgConfig - received {} keys", (configs == null ? 0 : configs.size()));
        if (configs == null || configs.isEmpty()) {
            log.warn("[POST] /api/orgConfig - empty body or wrong JSON shape (expected flat {\"k\":\"v\"})");
            return ResponseEntity.badRequest().body(Map.of("error", "Empty body or wrong JSON shape. Expect flat {\"key\":\"value\"}."));
        }

        // Pre-validation: Check all keys before processing (values can be null/empty)
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String k = entry.getKey();

            if (k == null || k.trim().isEmpty()) {
                log.error("[POST] /api/orgConfig - validation failed: empty or null key");
                return ResponseEntity.badRequest().body(Map.of("error", "Configuration key cannot be null or empty"));
            }
            if (k.length() > 500) {
                log.error("[POST] /api/orgConfig - validation failed: key too long");
                return ResponseEntity.badRequest().body(Map.of("error", "Configuration key exceeds maximum length of 500 characters"));
            }
            // Note: Values CAN be null or empty - no validation needed for values
        }

        try {
            int created = 0, updated = 0;
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                if (orgConfigService.existsByKey(key)) {
                    // Update existing
                    orgConfigService.updateConfig(key, value);
                    updated++;
                    log.info("[POST] /api/orgConfig - updated key: {}", key);
                } else {
                    // Create new
                    orgConfigService.setConfig(key, value);
                    created++;
                    log.info("[POST] /api/orgConfig - created key: {}", key);
                }
            }
            log.info("[POST] /api/orgConfig - created: {}, updated: {}", created, updated);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Created " + created + ", updated " + updated + " configurations",
                "created", String.valueOf(created),
                "updated", String.valueOf(updated)
            ));
        } catch (Exception e) {
            log.error("[POST] /api/orgConfig - failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> postFromJson(@RequestBody Map<String, Object> jsonConfig) {
        log.info("[POST] /api/orgConfig/json - nested JSON root keys: {}", (jsonConfig == null ? "null" : jsonConfig.keySet()));
        if (jsonConfig == null || jsonConfig.isEmpty()) {
            log.warn("[POST] /api/orgConfig/json - empty body");
            return ResponseEntity.badRequest().body(Map.of("error", "Empty body"));
        }
        try {
            Map<String, String> flatMap = orgConfigService.flattenMapPublic(jsonConfig);
            int created = 0, updated = 0;
            
            for (Map.Entry<String, String> entry : flatMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                if (orgConfigService.existsByKey(key)) {
                    orgConfigService.updateConfig(key, value);
                    updated++;
                } else {
                    orgConfigService.setConfig(key, value);
                    created++;
                }
            }
            
            log.info("[POST] /api/orgConfig/json - created: {}, updated: {}", created, updated);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Created " + created + ", updated " + updated + " configurations",
                "created", String.valueOf(created),
                "updated", String.valueOf(updated)
            ));
        } catch (Exception e) {
            log.error("[POST] /api/orgConfig/json - failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> updateMultipleConfigs(@RequestBody Map<String, String> configs) {
        log.info("[PUT] /api/orgConfig - received {} keys", (configs == null ? 0 : configs.size()));
        if (configs == null || configs.isEmpty()) {
            log.warn("[PUT] /api/orgConfig - empty body");
            return ResponseEntity.badRequest().body(Map.of("error", "Empty body"));
        }
        try {
            int updated = 0;
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                orgConfigService.setConfig(entry.getKey(), entry.getValue());
                updated++;
            }
            log.info("[PUT] /api/orgConfig - updated {} keys", updated);
            return ResponseEntity.ok(Map.of("message", "Updated " + updated + " configurations"));
        } catch (Exception e) {
            log.error("[PUT] /api/orgConfig - failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping(value = "/json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> updateFromJson(@RequestBody Map<String, Object> jsonConfig) {
        log.info("[PUT] /api/orgConfig/json - nested keys: {}", (jsonConfig == null ? "null" : jsonConfig.keySet()));
        if (jsonConfig == null || jsonConfig.isEmpty()) {
            log.warn("[PUT] /api/orgConfig/json - empty body");
            return ResponseEntity.badRequest().body(Map.of("error", "Empty body"));
        }
        try {
            orgConfigService.setConfigBulk(jsonConfig);
            log.info("[PUT] /api/orgConfig/json - updated OK");
            return ResponseEntity.ok(Map.of("message", "Configurations updated successfully"));
        } catch (Exception e) {
            log.error("[PUT] /api/orgConfig/json - failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, String>> deleteByKey(@PathVariable String key) {
        log.info("[DELETE] /api/orgConfig/{} - deleting", key);
        try {
            orgConfigService.deleteConfig(key);
            log.info("[DELETE] /api/orgConfig/{} - deleted", key);
            return ResponseEntity.ok(Map.of("message", "Deleted key: " + key));
        } catch (IllegalArgumentException e) {
            log.warn("[DELETE] /api/orgConfig/{} - not found", key);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("[DELETE] /api/orgConfig/{} - failed: {}", key, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
