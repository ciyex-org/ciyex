package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MaintenanceDto;
import com.qiaben.ciyex.service.MaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenances")
@RequiredArgsConstructor
@Slf4j
public class MaintenanceController {

    private final MaintenanceService service;

    // ✅ Create a new maintenance record
    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceDto>> create(@RequestBody MaintenanceDto dto) {
        try {
            MaintenanceDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                    .success(true)
                    .message("Maintenance created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create maintenance record", e);
            return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                    .success(false)
                    .message("Failed to create maintenance: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Retrieve maintenance by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceDto>> get(@PathVariable Long id) {
        try {
            MaintenanceDto maintenance = service.getById(id);
            if (maintenance == null) {
                return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                        .success(false)
                        .message("Maintenance not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                    .success(true)
                    .message("Maintenance retrieved successfully")
                    .data(maintenance)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve maintenance with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                    .success(false)
                    .message("Failed to retrieve maintenance: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Update maintenance record
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceDto>> update(@PathVariable Long id, @RequestBody MaintenanceDto dto) {
        try {
            MaintenanceDto updated = service.update(id, dto);
            if (updated == null) {
                return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                        .success(false)
                        .message("Maintenance not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                    .success(true)
                    .message("Maintenance updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update maintenance with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                    .success(false)
                    .message("Failed to update maintenance: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Delete maintenance by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Maintenance deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete maintenance with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete maintenance: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Get all maintenance records (with pagination)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<MaintenanceDto>>> getAll(
            @PageableDefault(sort = "id") Pageable pageable
    ) {
        try {
            Page<MaintenanceDto> list = service.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.<Page<MaintenanceDto>>builder()
                    .success(true)
                    .message("Maintenance list retrieved successfully")
                    .data(list)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve maintenance list", e);
            return ResponseEntity.ok(ApiResponse.<Page<MaintenanceDto>>builder()
                    .success(false)
                    .message("Failed to retrieve maintenance list: " + e.getMessage())
                    .build());
        }
    }

    // ✅ Update maintenance status (e.g., "pending", "completed")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MaintenanceDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        try {
            MaintenanceDto updated = service.updateStatus(id, status);
            if (updated == null) {
                return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                        .success(false)
                        .message("Maintenance not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                    .success(true)
                    .message("Maintenance status updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update maintenance status for id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                    .success(false)
                    .message("Failed to update maintenance status: " + e.getMessage())
                    .build());
        }
    }
}
