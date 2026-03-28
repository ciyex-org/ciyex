package org.ciyex.ehr.careplan.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.careplan.dto.CarePlanDto;
import org.ciyex.ehr.careplan.dto.CarePlanGoalDto;
import org.ciyex.ehr.careplan.dto.CarePlanInterventionDto;
import org.ciyex.ehr.careplan.service.CarePlanService;
import org.ciyex.ehr.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/CarePlan.read')")
@RestController
@RequestMapping("/api/care-plans")
@RequiredArgsConstructor
@Slf4j
public class CarePlanController {

    private final CarePlanService service;

    // ── Care Plan endpoints ──

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q) {
        try {
            if (q != null && !q.isBlank()) {
                var results = service.search(q);
                var pageResult = new PageImpl<>(results, PageRequest.of(page, size), results.size());
                return ResponseEntity.ok(ApiResponse.ok("Care plans retrieved", pageResult));
            }
            if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                Page<CarePlanDto> results = service.getByStatus(status, PageRequest.of(page, size));
                return ResponseEntity.ok(ApiResponse.ok("Care plans retrieved", results));
            }
            Page<CarePlanDto> results = service.getAll(PageRequest.of(page, size));
            return ResponseEntity.ok(ApiResponse.ok("Care plans retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list care plans", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> orgStats() {
        try {
            var stats = service.orgStats();
            return ResponseEntity.ok(ApiResponse.ok("Care plan stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get care plan stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<CarePlanDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var plans = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Care plans retrieved", plans));
        } catch (Exception e) {
            log.error("Failed to get care plans for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats/patient/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats(@PathVariable Long patientId) {
        try {
            var stats = service.stats(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Care plan stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get care plan stats for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarePlanDto>> getById(@PathVariable Long id) {
        try {
            var plan = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Care plan retrieved", plan));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get care plan {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/CarePlan.write')")
    public ResponseEntity<ApiResponse<CarePlanDto>> create(@RequestBody CarePlanDto dto) {
        try {
            var created = service.create(dto.getPatientId(), dto);
            return ResponseEntity.ok(ApiResponse.ok("Care plan created", created));
        } catch (Exception e) {
            log.error("Failed to create care plan", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/CarePlan.write')")
    public ResponseEntity<ApiResponse<CarePlanDto>> update(
            @PathVariable Long id, @RequestBody CarePlanDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Care plan updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update care plan {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/CarePlan.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Care plan deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete care plan {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Goal endpoints ──

    @PostMapping("/{id}/goals")
    @PreAuthorize("hasAuthority('SCOPE_user/CarePlan.write')")
    public ResponseEntity<ApiResponse<CarePlanGoalDto>> addGoal(
            @PathVariable Long id, @RequestBody CarePlanGoalDto dto) {
        try {
            var goal = service.addGoal(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Goal added", goal));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to add goal to care plan {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/goals/{goalId}")
    @PreAuthorize("hasAuthority('SCOPE_user/CarePlan.write')")
    public ResponseEntity<ApiResponse<CarePlanGoalDto>> updateGoal(
            @PathVariable Long goalId, @RequestBody CarePlanGoalDto dto) {
        try {
            var goal = service.updateGoal(goalId, dto);
            return ResponseEntity.ok(ApiResponse.ok("Goal updated", goal));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update goal {}", goalId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/goals/{goalId}")
    @PreAuthorize("hasAuthority('SCOPE_user/CarePlan.write')")
    public ResponseEntity<ApiResponse<Void>> removeGoal(@PathVariable Long goalId) {
        try {
            service.removeGoal(goalId);
            return ResponseEntity.ok(ApiResponse.ok("Goal removed", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to remove goal {}", goalId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/goals")
    public ResponseEntity<ApiResponse<List<CarePlanGoalDto>>> listGoals(@PathVariable Long id) {
        try {
            var goals = service.getGoalsByPlan(id);
            return ResponseEntity.ok(ApiResponse.ok("Goals retrieved", goals));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list goals for care plan {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Intervention endpoints ──

    @GetMapping("/{id}/interventions")
    public ResponseEntity<ApiResponse<List<CarePlanInterventionDto>>> listInterventions(@PathVariable Long id) {
        try {
            var interventions = service.getInterventionsByPlan(id);
            return ResponseEntity.ok(ApiResponse.ok("Interventions retrieved", interventions));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to list interventions for care plan {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/interventions")
    @PreAuthorize("hasAuthority('SCOPE_user/CarePlan.write')")
    public ResponseEntity<ApiResponse<CarePlanInterventionDto>> addIntervention(
            @PathVariable Long id, @RequestBody CarePlanInterventionDto dto) {
        try {
            var intervention = service.addIntervention(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Intervention added", intervention));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to add intervention to care plan {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/interventions/{intId}")
    @PreAuthorize("hasAuthority('SCOPE_user/CarePlan.write')")
    public ResponseEntity<ApiResponse<CarePlanInterventionDto>> updateIntervention(
            @PathVariable Long intId, @RequestBody CarePlanInterventionDto dto) {
        try {
            var intervention = service.updateIntervention(intId, dto);
            return ResponseEntity.ok(ApiResponse.ok("Intervention updated", intervention));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update intervention {}", intId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/interventions/{intId}")
    @PreAuthorize("hasAuthority('SCOPE_user/CarePlan.write')")
    public ResponseEntity<ApiResponse<Void>> removeIntervention(@PathVariable Long intId) {
        try {
            service.removeIntervention(intId);
            return ResponseEntity.ok(ApiResponse.ok("Intervention removed", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to remove intervention {}", intId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
