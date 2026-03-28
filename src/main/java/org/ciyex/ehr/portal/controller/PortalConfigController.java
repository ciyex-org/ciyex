package org.ciyex.ehr.portal.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.portal.entity.PortalForm;
import org.ciyex.ehr.portal.service.PortalConfigService;
import org.ciyex.ehr.portal.service.PortalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal/config")
@RequiredArgsConstructor
@Slf4j
public class PortalConfigController {

    private final PortalService service;
    private final PortalConfigService formService;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ─── Config ───

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig() {
        try {
            var config = service.getConfig();
            return ResponseEntity.ok(ApiResponse.ok("Portal config retrieved", config));
        } catch (Exception e) {
            log.error("Failed to get portal config", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    @PutMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveConfig(@RequestBody Map<String, Object> config) {
        try {
            var saved = service.saveConfig(config);
            return ResponseEntity.ok(ApiResponse.ok("Portal config saved", saved));
        } catch (Exception e) {
            log.error("Failed to save portal config", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    @PatchMapping("/{section}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> patchConfig(
            @PathVariable String section,
            @RequestBody Object sectionData) {
        try {
            var existing = new java.util.HashMap<>(service.getConfig());
            existing.put(section, sectionData);
            var saved = service.saveConfig(existing);
            return ResponseEntity.ok(ApiResponse.ok("Portal config section saved", saved));
        } catch (Exception e) {
            log.error("Failed to patch portal config section={}", section, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ─── Portal Forms (Onboarding/Consent/Intake) ───

    @GetMapping("/forms")
    public ResponseEntity<ApiResponse<List<PortalForm>>> listForms() {
        try {
            var forms = formService.getAllForms(orgAlias());
            return ResponseEntity.ok(ApiResponse.ok("Forms retrieved", forms));
        } catch (Exception e) {
            log.error("Failed to list portal forms", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    @PostMapping("/forms")
    public ResponseEntity<ApiResponse<PortalForm>> createForm(@RequestBody PortalForm form) {
        try {
            var saved = formService.saveForm(orgAlias(), form);
            return ResponseEntity.ok(ApiResponse.ok("Form created", saved));
        } catch (Exception e) {
            log.error("Failed to create portal form", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    @PutMapping("/forms/{id}")
    public ResponseEntity<ApiResponse<PortalForm>> updateForm(
            @PathVariable Long id,
            @RequestBody PortalForm form) {
        try {
            form.setId(id);
            var saved = formService.saveForm(orgAlias(), form);
            return ResponseEntity.ok(ApiResponse.ok("Form updated", saved));
        } catch (Exception e) {
            log.error("Failed to update portal form id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    @DeleteMapping("/forms/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForm(@PathVariable Long id) {
        try {
            formService.deleteForm(orgAlias(), id);
            return ResponseEntity.ok(ApiResponse.ok("Form deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete portal form id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    @PatchMapping("/forms/{id}/toggle")
    public ResponseEntity<ApiResponse<PortalForm>> toggleForm(
            @PathVariable Long id,
            @RequestParam boolean active) {
        try {
            var form = formService.toggleForm(orgAlias(), id, active);
            return ResponseEntity.ok(ApiResponse.ok("Form toggled", form));
        } catch (Exception e) {
            log.error("Failed to toggle portal form id={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
