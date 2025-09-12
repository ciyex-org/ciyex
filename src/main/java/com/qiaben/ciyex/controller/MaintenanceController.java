package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MaintenanceDto;
import com.qiaben.ciyex.service.MaintenanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService service;

    public MaintenanceController(MaintenanceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MaintenanceDto>> create(@RequestBody MaintenanceDto dto) {
        return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                .success(true)
                .message("Maintenance created successfully")
                .data(service.create(dto))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                .success(true)
                .message("Maintenance retrieved successfully")
                .data(service.getById(id))
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MaintenanceDto>> update(@PathVariable Long id, @RequestBody MaintenanceDto dto) {
        return ResponseEntity.ok(ApiResponse.<MaintenanceDto>builder()
                .success(true)
                .message("Maintenance updated successfully")
                .data(service.update(id, dto))
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Maintenance deleted successfully")
                .data(null)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MaintenanceDto>>> getAll(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<MaintenanceDto>>builder()
                .success(true)
                .message("Maintenance list retrieved successfully")
                .data(service.getAll(pageable))
                .build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MaintenanceDto>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        MaintenanceDto updated = service.updateStatus(id, status);
        return ResponseEntity.ok(
                ApiResponse.<MaintenanceDto>builder()
                        .success(true)
                        .message("Status updated successfully")
                        .data(updated)
                        .build()
        );
    }

}
