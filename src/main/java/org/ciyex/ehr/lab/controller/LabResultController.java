package org.ciyex.ehr.lab.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.lab.dto.LabResultDto;
import org.ciyex.ehr.lab.service.LabResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.read')")
@RestController
@RequestMapping("/api/lab-results")
@RequiredArgsConstructor
@Slf4j
public class LabResultController {

    private final LabResultService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabResultDto>>> getAll() {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Results retrieved", service.getAll()));
        } catch (Exception e) {
            log.error("Failed to get all results", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<LabResultDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Results retrieved", service.getByPatient(patientId)));
        } catch (Exception e) {
            log.error("Failed to get results for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<LabResultDto>>> getByOrder(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Results retrieved", service.getByOrder(orderId)));
        } catch (Exception e) {
            log.error("Failed to get results for order {}", orderId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabResultDto>> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Result retrieved", service.getById(id)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.write')")
    public ResponseEntity<ApiResponse<LabResultDto>> create(@RequestBody LabResultDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Result created", service.create(dto)));
        } catch (Exception e) {
            log.error("Failed to create result", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.write')")
    public ResponseEntity<ApiResponse<LabResultDto>> update(@PathVariable Long id, @RequestBody LabResultDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Result updated", service.update(id, dto)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update result {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Result deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }

    // Trending: get values over time for a specific LOINC code
    @GetMapping("/patient/{patientId}/trend/{loincCode}")
    public ResponseEntity<ApiResponse<List<LabResultDto>>> getTrend(
            @PathVariable Long patientId, @PathVariable String loincCode) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Trend retrieved", service.getTrend(patientId, loincCode)));
        } catch (Exception e) {
            log.error("Failed to get trend", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // Panel view: results grouped by panel
    @GetMapping("/patient/{patientId}/panel/{panelName}")
    public ResponseEntity<ApiResponse<List<LabResultDto>>> getByPanel(
            @PathVariable Long patientId, @PathVariable String panelName) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Panel results retrieved", service.getByPanel(patientId, panelName)));
        } catch (Exception e) {
            log.error("Failed to get panel results", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // E-signature
    @PostMapping("/{id}/sign")
    @PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.write')")
    public ResponseEntity<ApiResponse<LabResultDto>> signResult(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String signedBy = body.getOrDefault("signedBy", "Unknown");
            return ResponseEntity.ok(ApiResponse.ok("Result signed", service.signResult(id, signedBy)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }
}
