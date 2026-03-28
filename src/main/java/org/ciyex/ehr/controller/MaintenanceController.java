package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.inventory.dto.InvMaintenanceDto;
import org.ciyex.ehr.inventory.service.InvMaintenanceService2;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/maintenances")
@RequiredArgsConstructor
@Slf4j
public class MaintenanceController {

    private final InvMaintenanceService2 maintenanceService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvMaintenanceDto>> create(@Valid @RequestBody InvMaintenanceDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Maintenance created", maintenanceService.create(dto)));
        } catch (Exception e) {
            log.error("Failed to create maintenance", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to create: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvMaintenanceDto>> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Maintenance retrieved", maintenanceService.getById(id)));
        } catch (Exception e) {
            log.error("Failed to get maintenance {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Maintenance not found: " + id));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvMaintenanceDto>> update(@PathVariable Long id, @Valid @RequestBody InvMaintenanceDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Maintenance updated", maintenanceService.update(id, dto)));
        } catch (Exception e) {
            log.error("Failed to update maintenance {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to update: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvMaintenanceDto>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Status updated", maintenanceService.updateStatus(id, status)));
        } catch (Exception e) {
            log.error("Failed to update status for {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to update status: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            maintenanceService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok("Maintenance deleted", null));
        } catch (Exception e) {
            log.error("Failed to delete maintenance {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed to delete: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvMaintenanceDto>>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("Maintenance list retrieved", maintenanceService.getAll(pageable)));
    }
}
