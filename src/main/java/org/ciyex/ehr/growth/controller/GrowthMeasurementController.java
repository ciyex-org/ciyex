package org.ciyex.ehr.growth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.growth.dto.GrowthMeasurementDto;
import org.ciyex.ehr.growth.service.GrowthMeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Observation.read')")
@RestController
@RequestMapping("/api/growth")
@RequiredArgsConstructor
@Slf4j
public class GrowthMeasurementController {

    private final GrowthMeasurementService service;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<GrowthMeasurementDto>>> getByPatient(
            @PathVariable Long patientId) {
        try {
            var measurements = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Growth measurements retrieved", measurements));
        } catch (Exception e) {
            log.error("Failed to get growth measurements for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GrowthMeasurementDto>> getById(@PathVariable Long id) {
        try {
            var measurement = service.getById(id);
            return ResponseEntity.ok(ApiResponse.ok("Growth measurement retrieved", measurement));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get growth measurement {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}/chart")
    public ResponseEntity<ApiResponse<List<GrowthMeasurementDto>>> getChartData(
            @PathVariable Long patientId) {
        try {
            var chartData = service.getChartData(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Chart data retrieved", chartData));
        } catch (Exception e) {
            log.error("Failed to get chart data for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/patient/{patientId}")
    @PreAuthorize("hasAuthority('SCOPE_user/Observation.write')")
    public ResponseEntity<ApiResponse<GrowthMeasurementDto>> create(
            @PathVariable Long patientId, @RequestBody GrowthMeasurementDto dto) {
        try {
            var created = service.create(patientId, dto);
            return ResponseEntity.ok(ApiResponse.ok("Growth measurement created", created));
        } catch (Exception e) {
            log.error("Failed to create growth measurement", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Observation.write')")
    public ResponseEntity<ApiResponse<GrowthMeasurementDto>> update(
            @PathVariable Long id, @RequestBody GrowthMeasurementDto dto) {
        try {
            var updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Growth measurement updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update growth measurement {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Observation.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Growth measurement deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete growth measurement {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
