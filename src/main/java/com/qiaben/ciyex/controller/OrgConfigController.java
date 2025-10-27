package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.service.OrgConfigService;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/org-configs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class OrgConfigController {

    private final OrgConfigService orgConfigService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;

    public OrgConfigController(OrgConfigService orgConfigService, OrgIntegrationConfigProvider integrationConfigProvider) {
        this.orgConfigService = orgConfigService;
        this.integrationConfigProvider = integrationConfigProvider;
    }

    @GetMapping
    public ResponseEntity<List<OrgConfig>> getAll() {
        return ResponseEntity.ok(orgConfigService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrgConfig> getById(@PathVariable Long id) {
        return orgConfigService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-org/{orgId}")
    public ResponseEntity<OrgConfig> getByOrgId(@PathVariable Long orgId) {
        return orgConfigService.findByOrgId(orgId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrgConfig> create(@RequestBody OrgConfig orgConfig) {
        try {
            OrgConfig created = orgConfigService.create(orgConfig);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrgConfig> update(@PathVariable Long id, @RequestBody OrgConfig orgConfig) {
        try {
            OrgConfig updated = orgConfigService.update(id, orgConfig);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            orgConfigService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update S3 Document Storage Configuration for an organization
     */
    @PutMapping("/{orgId}/s3-config")
    public ResponseEntity<String> updateS3Config(@PathVariable Long orgId, @RequestBody OrgIntegrationConfigProvider.S3Config s3Config) {
        try {
            integrationConfigProvider.updateS3DocumentStorage(orgId, s3Config);
            return ResponseEntity.ok("S3 configuration updated successfully for orgId: " + orgId);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Failed to update S3 config: " + ex.getMessage());
        }
    }
}
