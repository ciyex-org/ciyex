package org.ciyex.ehr.cds.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.cds.dto.CdsAlertLogDto;
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
@RequestMapping("/api/cds/alerts")
@RequiredArgsConstructor
@Slf4j
public class CdsAlertController {

    private final CdsService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<CdsAlertLogDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            var alerts = service.getAllAlerts(pageable);
            return ResponseEntity.ok(ApiResponse.ok("CDS alerts retrieved", alerts));
        } catch (Exception e) {
            log.error("Failed to list CDS alerts", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<CdsAlertLogDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var alerts = service.getAlertsByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient CDS alerts retrieved", alerts));
        } catch (Exception e) {
            log.error("Failed to get CDS alerts for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Flag.write')")
    public ResponseEntity<ApiResponse<CdsAlertLogDto>> logAlert(@RequestBody CdsAlertLogDto dto) {
        try {
            var alert = service.logAlert(dto.getRuleId(), dto.getPatientId(), dto);
            return ResponseEntity.ok(ApiResponse.ok("CDS alert logged", alert));
        } catch (Exception e) {
            log.error("Failed to log CDS alert", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAuthority('SCOPE_user/Flag.write')")
    public ResponseEntity<ApiResponse<CdsAlertLogDto>> acknowledge(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            var acknowledged = service.acknowledgeAlert(
                    id,
                    body.getOrDefault("action", "acknowledged"),
                    body.get("reason"),
                    body.get("actedBy")
            );
            return ResponseEntity.ok(ApiResponse.ok("CDS alert acknowledged", acknowledged));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to acknowledge CDS alert {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> stats() {
        try {
            var stats = service.alertStats();
            return ResponseEntity.ok(ApiResponse.ok("CDS alert stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get CDS alert stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
