package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MaintenanceDto;
import com.qiaben.ciyex.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenances")
@RequiredArgsConstructor
@Slf4j
public class MaintenanceController {

    private final MaintenanceService service;

    // ✅ Create a new maintenance record
    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceDto>> create(@Valid @RequestBody MaintenanceDto dto, BindingResult result) {
        try {
            log.debug("Create maintenance request received: externalId={} dto={}", dto.getExternalId(), dto);
            
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getFieldErrors().forEach(error -> {
                    log.debug("Validation error - Field: {}, Message: {}", error.getField(), error.getDefaultMessage());
                    errorMsg.append(error.getDefaultMessage()).append(", ");
                });
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                log.debug("Final error message: {}", errorMsg.toString());
                return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                        .success(false)
                        .message(errorMsg.toString())
                        .data(null)
                        .build());
            }

            MaintenanceDto created = service.create(dto);
            log.debug("Created maintenance DTO to return: {}", created);
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
    public ResponseEntity<ApiResponse<MaintenanceDto>> get(@PathVariable("id") String id) {
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
    public ResponseEntity<ApiResponse<MaintenanceDto>> update(@PathVariable("id") String id, @Valid @RequestBody MaintenanceDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getFieldErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                        .success(false)
                        .message(errorMsg.toString())
                        .data(null)
                        .build());
            }

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
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id) {
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
            @PageableDefault(size = 20, sort = "id") Pageable pageable
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
            @PathVariable("id") String id,
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