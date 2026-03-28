package org.ciyex.ehr.education.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.education.dto.PatientEducationAssignmentDto;
import org.ciyex.ehr.education.service.PatientEducationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Communication.read')")
@RestController
@RequestMapping("/api/education/assignments")
@RequiredArgsConstructor
@Slf4j
public class PatientEducationController {

    private final PatientEducationService service;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<PatientEducationAssignmentDto>>> getByPatient(
            @PathVariable Long patientId) {
        try {
            var assignments = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Assignments retrieved", assignments));
        } catch (Exception e) {
            log.error("Failed to get education assignments for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PatientEducationAssignmentDto>> getById(@PathVariable Long id) {
        try {
            var assignment = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Assignment retrieved", assignment));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get education assignment {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats/patient/{patientId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPatientStats(@PathVariable Long patientId) {
        try {
            var stats = service.getPatientStats(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient education stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get education stats for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<PatientEducationAssignmentDto>> assign(
            @RequestBody PatientEducationAssignmentDto dto) {
        try {
            var assignment = service.assign(dto);
            return ResponseEntity.ok(ApiResponse.ok("Education material assigned", assignment));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to assign education material", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/viewed")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<PatientEducationAssignmentDto>> markViewed(@PathVariable Long id) {
        try {
            var assignment = service.markViewed(id);
            return ResponseEntity.ok(ApiResponse.ok("Assignment marked as viewed", assignment));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to mark assignment {} as viewed", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/completed")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<PatientEducationAssignmentDto>> markCompleted(@PathVariable Long id) {
        try {
            var assignment = service.markCompleted(id);
            return ResponseEntity.ok(ApiResponse.ok("Assignment marked as completed", assignment));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to mark assignment {} as completed", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<PatientEducationAssignmentDto>> dismiss(@PathVariable Long id) {
        try {
            var assignment = service.dismiss(id);
            return ResponseEntity.ok(ApiResponse.ok("Assignment dismissed", assignment));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to dismiss assignment {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Assignment deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete assignment {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
