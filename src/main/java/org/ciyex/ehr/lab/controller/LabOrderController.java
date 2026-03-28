package org.ciyex.ehr.lab.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.lab.dto.LabOrderDto;
import org.ciyex.ehr.lab.service.LabOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.read')")
@RestController
@RequestMapping("/api/lab-order")
@RequiredArgsConstructor
@Slf4j
public class LabOrderController {

    private final LabOrderService service;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<LabOrderDto>>> search(@RequestParam(defaultValue = "") String q) {
        try {
            var results = service.search(q);
            return ResponseEntity.ok(ApiResponse.ok("Lab orders retrieved", results));
        } catch (Exception e) {
            log.error("Failed to search lab orders", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to search: " + e.getMessage()));
        }
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<LabOrderDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var orders = service.getByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Lab orders retrieved", orders));
        } catch (Exception e) {
            log.error("Failed to get lab orders for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{patientId}/{orderId}")
    public ResponseEntity<ApiResponse<LabOrderDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long orderId) {
        try {
            var order = service.getById(patientId, orderId);
            return ResponseEntity.ok(ApiResponse.ok("Lab order retrieved", order));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get lab order {}", orderId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{patientId}")
    @PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.write')")
    public ResponseEntity<ApiResponse<LabOrderDto>> create(
            @PathVariable Long patientId, @RequestBody LabOrderDto dto) {
        try {
            var created = service.create(patientId, dto);
            return ResponseEntity.ok(ApiResponse.ok("Lab order created", created));
        } catch (Exception e) {
            log.error("Failed to create lab order", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{patientId}/{orderId}")
    @PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.write')")
    public ResponseEntity<ApiResponse<LabOrderDto>> update(
            @PathVariable Long patientId, @PathVariable Long orderId, @RequestBody LabOrderDto dto) {
        try {
            var updated = service.update(patientId, orderId, dto);
            return ResponseEntity.ok(ApiResponse.ok("Lab order updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update lab order {}", orderId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{patientId}/{orderId}")
    @PreAuthorize("hasAuthority('SCOPE_user/DiagnosticReport.write')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long orderId) {
        try {
            service.delete(patientId, orderId);
            return ResponseEntity.ok(ApiResponse.ok("Lab order deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete lab order {}", orderId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
