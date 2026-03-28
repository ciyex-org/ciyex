package org.ciyex.ehr.task.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.task.dto.ClinicalTaskDto;
import org.ciyex.ehr.task.service.ClinicalTaskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Task.read')")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Slf4j
public class ClinicalTaskController {

    private final ClinicalTaskService service;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String taskType) {
        try {
            Page<ClinicalTaskDto> results = service.listFiltered(page, size, q, status, priority, taskType);
            return ResponseEntity.ok(ApiResponse.ok("Tasks retrieved", results));
        } catch (Exception e) {
            log.error("Failed to list tasks", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to list tasks: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClinicalTaskDto>> getById(@PathVariable Long id) {
        try {
            var task = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Task retrieved", task));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get task {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<ClinicalTaskDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var tasks = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient tasks retrieved", tasks));
        } catch (Exception e) {
            log.error("Failed to get tasks for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/assignee/{assignee}")
    public ResponseEntity<ApiResponse<List<ClinicalTaskDto>>> getByAssignee(@PathVariable String assignee) {
        try {
            var tasks = service.getByAssignee(assignee);
            return ResponseEntity.ok(ApiResponse.ok("Assignee tasks retrieved", tasks));
        } catch (Exception e) {
            log.error("Failed to get tasks for assignee {}", assignee, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        try {
            var stats = service.dashboardStats();
            return ResponseEntity.ok(ApiResponse.ok("Task stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get task stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Task.write')")
    public ResponseEntity<ApiResponse<ClinicalTaskDto>> create(@RequestBody ClinicalTaskDto dto) {
        try {
            var created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.ok("Task created", created));
        } catch (Exception e) {
            log.error("Failed to create task", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Task.write')")
    public ResponseEntity<ApiResponse<ClinicalTaskDto>> update(
            @PathVariable Long id, @RequestBody ClinicalTaskDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Task updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update task {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('SCOPE_user/Task.write')")
    public ResponseEntity<ApiResponse<ClinicalTaskDto>> complete(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String completedBy = body.getOrDefault("completedBy", "unknown");
            var completed = service.complete(id, completedBy);
            return ResponseEntity.ok(ApiResponse.ok("Task completed", completed));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to complete task {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Task.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Task deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete task {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
