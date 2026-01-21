package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.service.HealthcareServiceService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/healthcare-services")
@Slf4j
public class HealthcareServiceController {

    private final HealthcareServiceService service;

    @Autowired
    public HealthcareServiceController(HealthcareServiceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HealthcareServiceDto>> create(@Valid @RequestBody HealthcareServiceDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getFieldErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(new ApiResponse.Builder<HealthcareServiceDto>()
                        .success(false)
                        .message(errorMsg.toString())
                        .build());
            }

            HealthcareServiceDto createdService = service.create(dto);
            ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                    .success(true)
                    .message("Healthcare Service created successfully")
                    .data(createdService)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create Healthcare Service: {}", e.getMessage(), e);
            ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                    .success(false)
                    .message("Failed to create healthcare service: " + e.getMessage())
                    .build();
            return ResponseEntity.ok(response);
        }
    }



    @GetMapping
    public ResponseEntity<ApiResponse<List<HealthcareServiceDto>>> get() {
        try {
            List<HealthcareServiceDto> services = service.getAll();
            ApiResponse<List<HealthcareServiceDto>> response = new ApiResponse.Builder<List<HealthcareServiceDto>>()
                    .success(true)
                    .message("Healthcare Services fetched successfully")
                    .data(services)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve Healthcare Services: {}", e.getMessage(), e);
            ApiResponse<List<HealthcareServiceDto>> response = new ApiResponse.Builder<List<HealthcareServiceDto>>()
                    .success(false)
                    .message("Failed to retrieve healthcare services: " + e.getMessage())
                    .build();
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HealthcareServiceDto>> getById(@PathVariable("id") String id) {
        try {
            HealthcareServiceDto service = this.service.getById(id);
            ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                    .success(true)
                    .message("Healthcare Service retrieved successfully")
                    .data(service)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve Healthcare Service with id {}: {}", id, e.getMessage(), e);
            ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                    .success(false)
                    .message("Failed to retrieve healthcare service: " + e.getMessage())
                    .build();
            return ResponseEntity.ok(response);
        }
    }




    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HealthcareServiceDto>> update(
            @PathVariable("id") String id,
            @Valid @RequestBody HealthcareServiceDto dto,
            BindingResult result
    ) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getFieldErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(new ApiResponse.Builder<HealthcareServiceDto>()
                        .success(false)
                        .message(errorMsg.toString())
                        .build());
            }

            HealthcareServiceDto updatedService = service.update(id, dto);
            ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                    .success(true)
                    .message("Healthcare Service updated successfully")
                    .data(updatedService)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update Healthcare Service with id {}: {}", id, e.getMessage(), e);
            String errorMessage = e.getMessage().contains("not found") || e.getMessage().contains("Not Found") || e.getMessage().contains("is not known")
                    ? "Healthcare service with ID " + id + " not found"
                    : "Failed to update healthcare service: " + e.getMessage();
            ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                    .success(false)
                    .message(errorMessage)
                    .build();
            return ResponseEntity.ok(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id) {
        try {
            service.delete(id);
            ApiResponse<Void> response = new ApiResponse.Builder<Void>()
                    .success(true)
                    .message("Healthcare Service deleted successfully")
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to delete Healthcare Service with id {}: {}", id, e.getMessage(), e);
            String errorMessage = e.getMessage().contains("not found") || e.getMessage().contains("Not Found") || e.getMessage().contains("is not known")
                    ? "Healthcare service with ID " + id + " not found"
                    : "Failed to delete healthcare service: " + e.getMessage();
            ApiResponse<Void> response = new ApiResponse.Builder<Void>()
                    .success(false)
                    .message(errorMessage)
                    .build();
            return ResponseEntity.ok(response);
        }
    }
}
