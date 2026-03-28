package org.ciyex.ehr.cds.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.cds.dto.CdsRuleDto;
import org.ciyex.ehr.cds.service.CdsService;
import org.ciyex.ehr.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Flag.read')")
@RestController
@RequestMapping("/api/cds/rules")
@RequiredArgsConstructor
@Slf4j
public class CdsRuleController {

    private final CdsService service;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Flag.write')")
    public ResponseEntity<ApiResponse<CdsRuleDto>> create(@RequestBody CdsRuleDto dto) {
        try {
            var created = service.createRule(dto);
            return ResponseEntity.ok(ApiResponse.ok("CDS rule created", created));
        } catch (Exception e) {
            log.error("Failed to create CDS rule", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CdsRuleDto>> getById(@PathVariable Long id) {
        try {
            var rule = service.getRuleById(id);
            return ResponseEntity.ok(ApiResponse.ok("CDS rule retrieved", rule));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get CDS rule {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CdsRuleDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            var rules = service.getAllRules(pageable);
            return ResponseEntity.ok(ApiResponse.ok("CDS rules retrieved", rules));
        } catch (Exception e) {
            log.error("Failed to list CDS rules", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CdsRuleDto>>> search(@RequestParam(defaultValue = "") String q) {
        try {
            var results = service.searchRules(q);
            return ResponseEntity.ok(ApiResponse.ok("CDS rules retrieved", results));
        } catch (Exception e) {
            log.error("Failed to search CDS rules", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Flag.write')")
    public ResponseEntity<ApiResponse<CdsRuleDto>> update(
            @PathVariable Long id, @RequestBody CdsRuleDto dto) {
        try {
            var updated = service.updateRule(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("CDS rule updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update CDS rule {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Flag.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.deleteRule(id);
            return ResponseEntity.ok(ApiResponse.ok("CDS rule deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete CDS rule {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('SCOPE_user/Flag.write')")
    public ResponseEntity<ApiResponse<CdsRuleDto>> toggleActive(@PathVariable Long id) {
        try {
            var toggled = service.toggleActive(id);
            return ResponseEntity.ok(ApiResponse.ok("CDS rule toggled", toggled));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to toggle CDS rule {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CdsRuleDto>>> getActiveByTrigger(
            @RequestParam String trigger) {
        try {
            var rules = service.evaluateForPatient(null, trigger);
            return ResponseEntity.ok(ApiResponse.ok("Active CDS rules retrieved", rules));
        } catch (Exception e) {
            log.error("Failed to get active CDS rules for trigger {}", trigger, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
