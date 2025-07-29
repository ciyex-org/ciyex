package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.service.OrgConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/org-configs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class OrgConfigController {

    private final OrgConfigService orgConfigService;

    public OrgConfigController(OrgConfigService orgConfigService) {
        this.orgConfigService = orgConfigService;
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
}
