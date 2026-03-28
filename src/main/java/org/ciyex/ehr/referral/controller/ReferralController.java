package org.ciyex.ehr.referral.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.referral.dto.ReferralDto;
import org.ciyex.ehr.referral.service.ReferralService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.read')")
@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
@Slf4j
public class ReferralController {

    private final ReferralService service;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (q != null && !q.isBlank()) {
                var results = service.search(q);
                return ResponseEntity.ok(ApiResponse.ok("Referrals retrieved", results));
            }
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            var results = service.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.ok("Referrals retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list referrals", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to list referrals: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReferralDto>> getById(@PathVariable Long id) {
        try {
            var referral = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Referral retrieved", referral));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get referral {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<ReferralDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var referrals = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient referrals retrieved", referrals));
        } catch (Exception e) {
            log.error("Failed to get referrals for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        try {
            var stats = service.getStats();
            return ResponseEntity.ok(ApiResponse.ok("Referral stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get referral stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.write')")
    public ResponseEntity<ApiResponse<ReferralDto>> create(@RequestBody ReferralDto dto) {
        try {
            var created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.ok("Referral created", created));
        } catch (Exception e) {
            log.error("Failed to create referral", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.write')")
    public ResponseEntity<ApiResponse<ReferralDto>> update(
            @PathVariable Long id, @RequestBody ReferralDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Referral updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update referral {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.write')")
    public ResponseEntity<ApiResponse<ReferralDto>> updateStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Status is required"));
            }
            var updated = service.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.ok("Referral status updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update referral status {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/ServiceRequest.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Referral deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete referral {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
