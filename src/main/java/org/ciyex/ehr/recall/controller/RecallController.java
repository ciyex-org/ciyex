package org.ciyex.ehr.recall.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.recall.dto.PatientRecallDto;
import org.ciyex.ehr.recall.dto.RecallKpiDto;
import org.ciyex.ehr.recall.dto.RecallOutreachLogDto;
import org.ciyex.ehr.recall.dto.RecallTypeDto;
import org.ciyex.ehr.recall.service.RecallService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Appointment.read')")
@RestController
@RequiredArgsConstructor
@Slf4j
public class RecallController {

    private final RecallService service;

    // ═══════════════════════════════════════════════════════
    // PATIENT RECALLS
    // ═══════════════════════════════════════════════════════

    @GetMapping("/api/recalls")
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long typeId,
            @RequestParam(required = false) Long providerId,
            @RequestParam(required = false) String dueDateFrom,
            @RequestParam(required = false) String dueDateTo,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<PatientRecallDto> results = service.getRecalls(
                    status, typeId, providerId, dueDateFrom, dueDateTo, search, page, size);
            return ResponseEntity.ok(ApiResponse.ok("Recalls retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list recalls", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to list recalls: " + e.getMessage()));
        }
    }

    @GetMapping("/api/recalls/kpis")
    public ResponseEntity<ApiResponse<RecallKpiDto>> getKpis() {
        try {
            var kpis = service.getKpis();
            return ResponseEntity.ok(ApiResponse.ok("Recall KPIs retrieved", kpis));
        } catch (Exception e) {
            log.error("Failed to get recall KPIs", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/api/recalls/{id}")
    public ResponseEntity<ApiResponse<PatientRecallDto>> getById(@PathVariable Long id) {
        try {
            var recall = service.getRecallById(id);
            return ResponseEntity.ok(ApiResponse.ok("Recall retrieved", recall));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get recall {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/api/recalls/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientRecallDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var recalls = service.getRecallsByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient recalls retrieved", recalls));
        } catch (Exception e) {
            log.error("Failed to get recalls for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api/recalls")
    @PreAuthorize("hasAuthority('SCOPE_user/Appointment.write')")
    public ResponseEntity<ApiResponse<PatientRecallDto>> create(@RequestBody PatientRecallDto dto) {
        try {
            var created = service.createRecall(dto);
            return ResponseEntity.ok(ApiResponse.ok("Recall created", created));
        } catch (Exception e) {
            log.error("Failed to create recall", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/api/recalls/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Appointment.write')")
    public ResponseEntity<ApiResponse<PatientRecallDto>> update(
            @PathVariable Long id, @RequestBody PatientRecallDto dto) {
        try {
            var updated = service.updateRecall(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Recall updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update recall {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/api/recalls/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Appointment.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.deleteRecall(id);
            return ResponseEntity.ok(ApiResponse.ok("Recall deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete recall {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api/recalls/{id}/outreach")
    @PreAuthorize("hasAuthority('SCOPE_user/Appointment.write')")
    public ResponseEntity<ApiResponse<RecallOutreachLogDto>> logOutreach(
            @PathVariable Long id, @RequestBody RecallOutreachLogDto dto) {
        try {
            var logged = service.logOutreach(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Outreach logged", logged));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to log outreach for recall {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════
    // RECALL TYPES
    // ═══════════════════════════════════════════════════════

    @GetMapping("/api/recall-types")
    public ResponseEntity<ApiResponse<List<RecallTypeDto>>> listTypes(
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        try {
            var types = all ? service.getAllRecallTypes() : service.getRecallTypes();
            return ResponseEntity.ok(ApiResponse.ok("Recall types retrieved", types));
        } catch (Exception e) {
            log.error("Failed to list recall types", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/api/recall-types")
    @PreAuthorize("hasAuthority('SCOPE_user/Appointment.write')")
    public ResponseEntity<ApiResponse<RecallTypeDto>> createType(@RequestBody RecallTypeDto dto) {
        try {
            var created = service.createRecallType(dto);
            return ResponseEntity.ok(ApiResponse.ok("Recall type created", created));
        } catch (Exception e) {
            log.error("Failed to create recall type", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/api/recall-types/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Appointment.write')")
    public ResponseEntity<ApiResponse<RecallTypeDto>> updateType(
            @PathVariable Long id, @RequestBody RecallTypeDto dto) {
        try {
            var updated = service.updateRecallType(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Recall type updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update recall type {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
