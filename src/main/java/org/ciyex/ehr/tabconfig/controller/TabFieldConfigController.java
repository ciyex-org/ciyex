package org.ciyex.ehr.tabconfig.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.settings.service.UiColorConfigService;
import org.ciyex.ehr.tabconfig.entity.TabFieldConfig;
import org.ciyex.ehr.tabconfig.service.TabFieldConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

@Slf4j
@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/tab-field-config")
@RequiredArgsConstructor
public class TabFieldConfigController {

    private final TabFieldConfigService service;
    private final ObjectMapper objectMapper;
    private final UiColorConfigService colorService;

    private String getOrgId() {
        try {
            RequestContext ctx = RequestContext.get();
            return ctx != null && ctx.getOrgName() != null ? ctx.getOrgName() : "*";
        } catch (Exception e) {
            return "*";
        }
    }

    /**
     * GET /api/tab-field-config/tabs — List all available tab keys with FHIR resources
     */
    @GetMapping("/tabs")
    public ResponseEntity<List<Map<String, Object>>> listTabs() {
        return ResponseEntity.ok(service.listAvailableTabs());
    }

    /**
     * GET /api/tab-field-config/{tabKey} — Get effective field config for a tab
     * Query params: practiceType (optional, defaults to *)
     */
    @GetMapping("/{tabKey}")
    public ResponseEntity<?> getFieldConfig(
            @PathVariable String tabKey,
            @RequestParam(name = "practiceType", defaultValue = "*") String practiceType) {
        TabFieldConfig cfg = service.getEffectiveFieldConfig(tabKey, practiceType, getOrgId());
        if (cfg == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("tabKey", tabKey);
            empty.put("practiceTypeCode", practiceType);
            empty.put("orgId", getOrgId());
            empty.put("fieldConfig", List.of());
            empty.put("fhirResources", List.of());
            return ResponseEntity.ok(empty);
        }
        return ResponseEntity.ok(toResponse(cfg));
    }

    /**
     * GET /api/tab-field-config/{tabKey}/practice-type/{code} — Get practice type defaults
     */
    @GetMapping("/{tabKey}/practice-type/{code}")
    public ResponseEntity<?> getPracticeTypeFieldConfig(
            @PathVariable String tabKey,
            @PathVariable String code) {
        TabFieldConfig cfg = service.getPracticeTypeFieldConfig(tabKey, code);
        if (cfg == null) {
            // Fall back to universal
            cfg = service.getPracticeTypeFieldConfig(tabKey, "*");
        }
        if (cfg == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(cfg));
    }

    /**
     * GET /api/tab-field-config/all — List all field configs for a practice type
     * Query params: practiceType (optional, defaults to *)
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> listAllFieldConfigs(
            @RequestParam(name = "practiceType", defaultValue = "*") String practiceType) {
        List<TabFieldConfig> configs = service.listFieldConfigsForPracticeType(practiceType, getOrgId());
        List<Map<String, Object>> result = configs.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/tab-field-config/{tabKey} — Save org-specific field config override
     */
    @PutMapping("/{tabKey}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<?> saveFieldConfig(@PathVariable String tabKey, @RequestBody Map<String, Object> body) {
        try {
            String practiceTypeCode = (String) body.getOrDefault("practiceTypeCode", "*");
            Object fhirRes = body.get("fhirResources");
            Object fieldCfg = body.get("fieldConfig");
            String apiBasePath = body.containsKey("apiBasePath") ? (String) body.get("apiBasePath") : null;
            String category = body.containsKey("category") ? (String) body.get("category") : null;

            String fhirResourcesJson = fhirRes != null ? objectMapper.writeValueAsString(fhirRes) : null;
            String fieldConfigJson = objectMapper.writeValueAsString(fieldCfg);

            TabFieldConfig saved = service.saveOrgFieldConfig(tabKey, getOrgId(), practiceTypeCode, fhirResourcesJson, fieldConfigJson, apiBasePath, category);

            // Auto-assign colors for any new appointment type options
            if ("appointments".equals(tabKey) && fieldCfg != null) {
                try {
                    colorService.autoAssignColorsForAppointmentOptions(getOrgId(), fieldCfg);
                } catch (Exception e) {
                    log.warn("Failed to auto-assign visit type colors: {}", e.getMessage());
                }
            }

            return ResponseEntity.ok(toResponse(saved));
        } catch (Exception e) {
            log.error("Failed to save field config for tab: {}", tabKey, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/tab-field-config/{tabKey} — Reset org override (revert to defaults)
     */
    @DeleteMapping("/{tabKey}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Void> resetFieldConfig(@PathVariable String tabKey) {
        service.resetOrgFieldConfig(tabKey, getOrgId());
        return ResponseEntity.ok().build();
    }

    // ==================== Layout endpoints (replaces /api/tab-config) ====================

    /**
     * GET /api/tab-field-config/layout — Get effective tab layout grouped by category.
     * Replaces GET /api/tab-config/effective.
     */
    @GetMapping("/layout")
    public ResponseEntity<Map<String, Object>> getLayout() {
        return ResponseEntity.ok(service.getLayout(getOrgId()));
    }

    /**
     * PUT /api/tab-field-config/layout — Save org-specific tab layout.
     * Replaces PUT /api/tab-config/org.
     */
    @PutMapping("/layout")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<?> saveLayout(@RequestBody Map<String, Object> body) {
        try {
            Object tabConfig = body.get("tabConfig");
            if (tabConfig == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "tabConfig is required"));
            }
            String layoutJson = objectMapper.writeValueAsString(tabConfig);
            service.saveLayout(getOrgId(), layoutJson);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to save tab layout", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/tab-field-config/layout — Reset org layout to defaults.
     * Replaces DELETE /api/tab-config/org.
     */
    @DeleteMapping("/layout")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Void> resetLayout() {
        service.resetLayout(getOrgId());
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> toResponse(TabFieldConfig cfg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", cfg.getId());
        map.put("tabKey", cfg.getTabKey());
        map.put("practiceTypeCode", cfg.getPracticeTypeCode());
        map.put("orgId", cfg.getOrgId());
        map.put("label", cfg.getLabel());
        map.put("icon", cfg.getIcon());
        map.put("category", cfg.getCategory());
        map.put("position", cfg.getPosition());
        map.put("visible", cfg.getVisible());
        map.put("apiBasePath", cfg.getApiBasePath());
        map.put("version", cfg.getVersion());
        try {
            map.put("fhirResources", objectMapper.readValue(cfg.getFhirResources(), Object.class));
            map.put("fieldConfig", objectMapper.readValue(cfg.getFieldConfig(), Object.class));
        } catch (Exception e) {
            map.put("fhirResources", cfg.getFhirResources());
            map.put("fieldConfig", cfg.getFieldConfig());
        }
        return map;
    }
}
