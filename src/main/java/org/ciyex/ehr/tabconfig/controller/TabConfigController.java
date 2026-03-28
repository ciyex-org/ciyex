package org.ciyex.ehr.tabconfig.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.tabconfig.entity.CustomTab;
import org.ciyex.ehr.tabconfig.entity.PracticeType;
import org.ciyex.ehr.tabconfig.service.TabConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

@Slf4j
@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/tab-config")
@RequiredArgsConstructor
public class TabConfigController {

    private final TabConfigService tabConfigService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    private String getOrgId() {
        try {
            RequestContext ctx = RequestContext.get();
            return ctx != null && ctx.getOrgName() != null ? ctx.getOrgName() : "*";
        } catch (Exception e) {
            return "*";
        }
    }

    // ---- Practice Types ----

    @GetMapping("/practice-types")
    public ResponseEntity<List<PracticeType>> listPracticeTypes() {
        return ResponseEntity.ok(tabConfigService.listPracticeTypes(getOrgId()));
    }

    @GetMapping("/practice-types/{code}")
    public ResponseEntity<PracticeType> getPracticeType(@PathVariable String code) {
        return tabConfigService.getPracticeType(code, getOrgId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/practice-types/{code}/defaults")
    public ResponseEntity<Map<String, Object>> getPracticeTypeDefaults(@PathVariable String code) {
        return ResponseEntity.ok(tabConfigService.getPracticeTypeDefaults(code, getOrgId()));
    }

    @GetMapping("/practice-types/{code}/specialties")
    public ResponseEntity<List<String>> getPracticeTypeSpecialties(@PathVariable String code) {
        List<String> specs = jdbcTemplate.queryForList(
                "SELECT specialty_code FROM practice_type_specialty WHERE practice_type_code = ?",
                String.class, code);
        return ResponseEntity.ok(specs);
    }

    @PostMapping("/practice-types")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<PracticeType> createPracticeType(@RequestBody PracticeType pt) {
        pt.setOrgId(getOrgId());
        return ResponseEntity.ok(tabConfigService.createPracticeType(pt));
    }

    @PutMapping("/practice-types/{code}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<PracticeType> updatePracticeType(@PathVariable String code, @RequestBody PracticeType pt) {
        return ResponseEntity.ok(tabConfigService.updatePracticeType(code, getOrgId(), pt));
    }

    @DeleteMapping("/practice-types/{code}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Void> deletePracticeType(@PathVariable String code) {
        tabConfigService.deletePracticeType(code, getOrgId());
        return ResponseEntity.ok().build();
    }

    // ---- Effective Config ----

    @GetMapping("/effective")
    public ResponseEntity<Map<String, Object>> getEffectiveConfig() {
        return ResponseEntity.ok(tabConfigService.getEffectiveConfig(getOrgId()));
    }

    @PostMapping("/org/clone-from-default")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Map<String, Object>> cloneFromDefault(@RequestBody Map<String, String> body) {
        String code = body.get("practiceTypeCode");
        return ResponseEntity.ok(tabConfigService.cloneFromDefault(getOrgId(), code));
    }

    @PutMapping("/org")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Void> saveOrgConfig(@RequestBody Map<String, Object> body) {
        try {
            Object tabConfig = body.get("tabConfig");
            String json = objectMapper.writeValueAsString(tabConfig);
            tabConfigService.saveOrgConfig(getOrgId(), json);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to save org config", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/org")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Void> deleteOrgConfig() {
        tabConfigService.deleteOrgConfig(getOrgId());
        return ResponseEntity.ok().build();
    }

    // ---- Custom Tabs ----

    @GetMapping("/custom-tabs")
    public ResponseEntity<List<CustomTab>> listCustomTabs() {
        return ResponseEntity.ok(tabConfigService.listCustomTabs(getOrgId()));
    }

    @PostMapping("/custom-tabs")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<CustomTab> createCustomTab(@RequestBody CustomTab tab) {
        tab.setOrgId(getOrgId());
        return ResponseEntity.ok(tabConfigService.createCustomTab(tab));
    }

    @PutMapping("/custom-tabs/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<CustomTab> updateCustomTab(@PathVariable UUID id, @RequestBody CustomTab tab) {
        return ResponseEntity.ok(tabConfigService.updateCustomTab(id, tab));
    }

    @DeleteMapping("/custom-tabs/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Void> deleteCustomTab(@PathVariable UUID id) {
        tabConfigService.deleteCustomTab(id);
        return ResponseEntity.ok().build();
    }
}
