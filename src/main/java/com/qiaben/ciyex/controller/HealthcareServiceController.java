package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.service.HealthcareServiceService;
import com.qiaben.ciyex.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/healthcare-services")
public class HealthcareServiceController {

    private final HealthcareServiceService healthcareServiceService;

    // Corrected constructor to use HealthcareServiceService
    public HealthcareServiceController(HealthcareServiceService healthcareServiceService) {
        this.healthcareServiceService = healthcareServiceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HealthcareServiceDto>> create(@RequestBody HealthcareServiceDto dto) {
        HealthcareServiceDto createdHealthcareService = healthcareServiceService.create(dto);
        ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                .success(true)
                .message("Healthcare Service created successfully")
                .data(createdHealthcareService)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HealthcareServiceDto>> getById(@PathVariable Long id) {
        HealthcareServiceDto healthcareService = healthcareServiceService.getById(id);
        ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                .success(true)
                .message("Healthcare Service fetched successfully")
                .data(healthcareService)
                .build();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HealthcareServiceDto>> update(@PathVariable Long id, @RequestBody HealthcareServiceDto dto) {
        HealthcareServiceDto updatedHealthcareService = healthcareServiceService.update(id, dto);
        ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                .success(true)
                .message("Healthcare Service updated successfully")
                .data(updatedHealthcareService)
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        healthcareServiceService.delete(id);
        ApiResponse<Void> response = new ApiResponse.Builder<Void>()
                .success(true)
                .message("Healthcare Service deleted successfully")
                .data(null)
                .build();
        return ResponseEntity.ok(response);
    }
}
