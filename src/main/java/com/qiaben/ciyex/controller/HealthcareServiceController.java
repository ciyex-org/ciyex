package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.service.HealthcareServiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<HealthcareServiceDto>> create(@RequestBody HealthcareServiceDto dto) {
        try {
            // Validate mandatory fields
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                        .success(false)
                        .message(validationError)
                        .build();
                return ResponseEntity.ok(response);
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
            @RequestBody HealthcareServiceDto dto
    ) {
        try {
            // Validate mandatory fields
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                        .success(false)
                        .message(validationError)
                        .build();
                return ResponseEntity.ok(response);
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
            ApiResponse<HealthcareServiceDto> response = new ApiResponse.Builder<HealthcareServiceDto>()
                    .success(false)
                    .message("Failed to update healthcare service: " + e.getMessage())
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
            ApiResponse<Void> response = new ApiResponse.Builder<Void>()
                    .success(false)
                    .message("Failed to delete healthcare service: " + e.getMessage())
                    .build();
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Validates mandatory fields for HealthcareService creation and update
     * @param dto HealthcareServiceDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(HealthcareServiceDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            missingFields.append("name, ");
        }

        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty()) {
            missingFields.append("location, ");
        }

        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            missingFields.append("type, ");
        }

        if (dto.getHoursOfOperation() == null || dto.getHoursOfOperation().trim().isEmpty()) {
            missingFields.append("hoursOfOperation, ");
        }

        if (!missingFields.isEmpty()) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }

}
