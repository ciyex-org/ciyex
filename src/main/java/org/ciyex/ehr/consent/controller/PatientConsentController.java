package org.ciyex.ehr.consent.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.consent.dto.PatientConsentDto;
import org.ciyex.ehr.consent.service.PatientConsentService;
import org.ciyex.ehr.dto.ApiResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Consent.read')")
@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
@Slf4j
public class PatientConsentController {

    private final PatientConsentService service;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (q != null && !q.isBlank()) {
                var results = service.search(q);
                return ResponseEntity.ok(ApiResponse.ok("Consents retrieved", results));
            }
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            var results = service.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.ok("Consents retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list consents", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to list consents: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientConsentDto>> getById(@PathVariable Long id) {
        try {
            var consent = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Consent retrieved", consent));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get consent {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientConsentDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var consents = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient consents retrieved", consents));
        } catch (Exception e) {
            log.error("Failed to get consents for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        try {
            var stats = service.getStats();
            return ResponseEntity.ok(ApiResponse.ok("Consent stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get consent stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Consent.write')")
    public ResponseEntity<ApiResponse<PatientConsentDto>> create(@RequestBody PatientConsentDto dto) {
        try {
            var created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.ok("Consent created", created));
        } catch (Exception e) {
            log.error("Failed to create consent", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Consent.write')")
    public ResponseEntity<ApiResponse<PatientConsentDto>> update(
            @PathVariable Long id, @RequestBody PatientConsentDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Consent updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update consent {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize("hasAuthority('SCOPE_user/Consent.write')")
    public ResponseEntity<ApiResponse<PatientConsentDto>> sign(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String signedBy = body.get("signedBy");
            String witnessName = body.get("witnessName");
            if (signedBy == null || signedBy.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("signedBy is required"));
            }
            var signed = service.sign(id, signedBy, witnessName);
            return ResponseEntity.ok(ApiResponse.ok("Consent signed", signed));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to sign consent {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('SCOPE_user/Consent.write')")
    public ResponseEntity<ApiResponse<PatientConsentDto>> revoke(@PathVariable Long id) {
        try {
            var revoked = service.revoke(id);
            return ResponseEntity.ok(ApiResponse.ok("Consent revoked", revoked));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to revoke consent {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Consent.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Consent deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete consent {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
