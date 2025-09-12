package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ServicebillDto;
import com.qiaben.ciyex.service.ServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Slf4j
public class ServiceController {

    private final ServiceService serviceService;

    /* ------------------- CREATE ------------------- */
    @PostMapping
    public ResponseEntity<ApiResponse<ServicebillDto>> create(@RequestBody ServicebillDto dto) {
        try {
            ServicebillDto created = serviceService.create(dto);
            return ResponseEntity.ok(ApiResponse.<ServicebillDto>builder()
                    .success(true)
                    .message("Service created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create service: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<ServicebillDto>builder()
                    .success(false)
                    .message("Failed to create service: " + e.getMessage())
                    .build());
        }
    }

    /* ------------------- GET ONE ------------------- */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServicebillDto>> getById(@PathVariable Long id) {
        try {
            ServicebillDto dto = serviceService.getById(id);
            return ResponseEntity.ok(ApiResponse.<ServicebillDto>builder()
                    .success(true)
                    .message("Service retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve service {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<ServicebillDto>builder()
                    .success(false)
                    .message("Failed to retrieve service: " + e.getMessage())
                    .build());
        }
    }

    /* ------------------- GET ALL ------------------- */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServicebillDto>>> getAll() {
        try {
            List<ServicebillDto> list = serviceService.getAll();
            return ResponseEntity.ok(ApiResponse.<List<ServicebillDto>>builder()
                    .success(true)
                    .message("Services retrieved successfully")
                    .data(list)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve services: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<ServicebillDto>>builder()
                    .success(false)
                    .message("Failed to retrieve services: " + e.getMessage())
                    .build());
        }
    }

    /* ------------------- UPDATE ------------------- */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServicebillDto>> update(@PathVariable Long id, @RequestBody ServicebillDto dto) {
        try {
            ServicebillDto updated = serviceService.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<ServicebillDto>builder()
                    .success(true)
                    .message("Service updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update service {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<ServicebillDto>builder()
                    .success(false)
                    .message("Failed to update service: " + e.getMessage())
                    .build());
        }
    }

    /* ------------------- DELETE ------------------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            serviceService.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Service deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete service {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete service: " + e.getMessage())
                    .build());
        }
    }
}
