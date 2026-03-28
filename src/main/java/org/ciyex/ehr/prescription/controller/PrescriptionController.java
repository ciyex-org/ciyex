package org.ciyex.ehr.prescription.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.prescription.dto.PrescriptionDto;
import org.ciyex.ehr.prescription.service.PrescriptionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/MedicationRequest.read')")
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Slf4j
public class PrescriptionController {

    private final PrescriptionService service;

    // ── List (paginated) or Search ──

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            if (q != null && !q.isBlank()) {
                var results = service.search(q);
                return ResponseEntity.ok(ApiResponse.ok("Prescriptions found", results));
            }
            boolean hasStatus = status != null && !status.isBlank() && !"all".equalsIgnoreCase(status);
            boolean hasPriority = priority != null && !priority.isBlank() && !"all".equalsIgnoreCase(priority);
            if (hasStatus && hasPriority) {
                var results = service.listByStatusAndPriority(status, priority, PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok("Prescriptions retrieved", results));
            }
            if (hasStatus) {
                var results = service.listByStatus(status, PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok("Prescriptions retrieved", results));
            }
            if (hasPriority) {
                var results = service.listByPriority(priority, PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok("Prescriptions retrieved", results));
            }
            var results = service.list(PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.ok("Prescriptions retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list prescriptions", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Get by ID ──

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PrescriptionDto>> getById(@PathVariable Long id) {
        try {
            var rx = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Prescription retrieved", rx));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get prescription {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Get by Patient ──

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var rxList = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient prescriptions retrieved", rxList));
        } catch (Exception e) {
            log.error("Failed to get prescriptions for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Get by Encounter ──

    @GetMapping("/encounter/{encounterId}")
    public ResponseEntity<ApiResponse<List<PrescriptionDto>>> getByEncounter(@PathVariable Long encounterId) {
        try {
            var rxList = service.getByEncounter(encounterId);
            return ResponseEntity.ok(ApiResponse.ok("Encounter prescriptions retrieved", rxList));
        } catch (Exception e) {
            log.error("Failed to get prescriptions for encounter {}", encounterId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Stats ──

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        try {
            var stats = service.stats();
            return ResponseEntity.ok(ApiResponse.ok("Prescription stats", stats));
        } catch (Exception e) {
            log.error("Failed to get prescription stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Create ──

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/MedicationRequest.write')")
    public ResponseEntity<ApiResponse<PrescriptionDto>> create(@RequestBody PrescriptionDto dto) {
        try {
            var created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.ok("Prescription created", created));
        } catch (Exception e) {
            log.error("Failed to create prescription", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Update ──

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/MedicationRequest.write')")
    public ResponseEntity<ApiResponse<PrescriptionDto>> update(
            @PathVariable Long id, @RequestBody PrescriptionDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Prescription updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update prescription {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Discontinue ──

    @PostMapping("/{id}/discontinue")
    @PreAuthorize("hasAuthority('SCOPE_user/MedicationRequest.write')")
    public ResponseEntity<ApiResponse<PrescriptionDto>> discontinue(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String reason = body.getOrDefault("reason", "No reason provided");
            var rx = service.discontinue(id, reason);
            return ResponseEntity.ok(ApiResponse.ok("Prescription discontinued", rx));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to discontinue prescription {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Refill ──

    @PostMapping("/{id}/refill")
    @PreAuthorize("hasAuthority('SCOPE_user/MedicationRequest.write')")
    public ResponseEntity<ApiResponse<PrescriptionDto>> refill(@PathVariable Long id) {
        try {
            var rx = service.refill(id);
            return ResponseEntity.ok(ApiResponse.ok("Prescription refilled", rx));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to refill prescription {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Delete ──

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/MedicationRequest.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Prescription deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete prescription {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
