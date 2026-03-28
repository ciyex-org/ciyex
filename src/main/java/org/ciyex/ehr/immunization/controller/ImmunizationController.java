package org.ciyex.ehr.immunization.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.immunization.dto.ImmunizationDto;
import org.ciyex.ehr.immunization.service.ImmunizationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Immunization.read')")
@RestController
@RequestMapping("/api/immunizations")
@RequiredArgsConstructor
@Slf4j
public class ImmunizationController {

    private final ImmunizationService service;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            if (q != null && !q.isBlank()) {
                var results = service.search(q);
                return ResponseEntity.ok(ApiResponse.ok("Immunizations retrieved", results));
            }
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "administrationDate"));
            var results = service.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.ok("Immunizations retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list immunizations", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to list immunizations: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<ImmunizationDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var records = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient immunizations retrieved", records));
        } catch (Exception e) {
            log.error("Failed to get immunizations for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ImmunizationDto>> getById(@PathVariable Long id) {
        try {
            var record = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Immunization record retrieved", record));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get immunization {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats/patient/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPatientStats(@PathVariable Long patientId) {
        try {
            var stats = service.getPatientStats(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Immunization stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get immunization stats for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Immunization.write')")
    public ResponseEntity<ApiResponse<ImmunizationDto>> create(@RequestBody ImmunizationDto dto) {
        try {
            var created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.ok("Immunization record created", created));
        } catch (Exception e) {
            log.error("Failed to create immunization record", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Immunization.write')")
    public ResponseEntity<ApiResponse<ImmunizationDto>> update(
            @PathVariable Long id, @RequestBody ImmunizationDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Immunization record updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update immunization {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Immunization.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Immunization record deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete immunization {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
