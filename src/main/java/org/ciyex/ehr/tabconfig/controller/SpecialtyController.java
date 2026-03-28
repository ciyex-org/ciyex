package org.ciyex.ehr.tabconfig.controller;

import lombok.RequiredArgsConstructor;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.tabconfig.entity.Specialty;
import org.ciyex.ehr.tabconfig.service.SpecialtyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    private String getOrgId() {
        try {
            RequestContext ctx = RequestContext.get();
            return ctx != null && ctx.getOrgName() != null ? ctx.getOrgName() : "*";
        } catch (Exception e) {
            return "*";
        }
    }

    @GetMapping
    public ResponseEntity<List<Specialty>> listSpecialties() {
        return ResponseEntity.ok(specialtyService.listSpecialties(getOrgId()));
    }

    @GetMapping("/{code}")
    public ResponseEntity<Specialty> getSpecialty(@PathVariable String code) {
        return specialtyService.getSpecialty(code, getOrgId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Specialty> createSpecialty(@RequestBody Specialty specialty) {
        specialty.setOrgId(getOrgId());
        return ResponseEntity.ok(specialtyService.createSpecialty(specialty));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Specialty> updateSpecialty(@PathVariable String code, @RequestBody Specialty specialty) {
        return ResponseEntity.ok(specialtyService.updateSpecialty(code, getOrgId(), specialty));
    }

    @DeleteMapping("/{code}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<Void> deleteSpecialty(@PathVariable String code) {
        specialtyService.deleteSpecialty(code, getOrgId());
        return ResponseEntity.ok().build();
    }
}
