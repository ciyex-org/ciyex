package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SupplierDto;
import com.qiaben.ciyex.service.SupplierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@Slf4j
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierDto>> create(@RequestBody SupplierDto dto) {
        try {
            // Validate mandatory fields
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                        .success(false)
                        .message(validationError)
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(true)
                    .message("Supplier created successfully")
                    .data(service.create(dto))
                    .build());
        } catch (Exception e) {
            log.error("Failed to create Supplier: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(false)
                    .message("Failed to create supplier: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto>> get(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(true)
                    .message("Supplier retrieved successfully")
                    .data(service.getById(id))
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Supplier with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(false)
                    .message("Failed to retrieve supplier: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto>> update(@PathVariable("id") String id, @RequestBody SupplierDto dto) {
        try {
            // Validate mandatory fields
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                        .success(false)
                        .message(validationError)
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(true)
                    .message("Supplier updated successfully")
                    .data(service.update(id, dto))
                    .build());
        } catch (Exception e) {
            log.error("Failed to update Supplier with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(false)
                    .message("Failed to update supplier: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Supplier deleted successfully")
                    .data(null)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete Supplier with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete supplier: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupplierDto>>> getAll(@PageableDefault Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.<Page<SupplierDto>>builder()
                .success(true)
                .message("Suppliers retrieved successfully")
                .data(service.getAll(pageable))
                .build());
    }
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .success(true)
                .message("Supplier count retrieved successfully")
                .data(service.countByOrg())
                .build());
    }

    /**
     * Validates mandatory fields for Supplier creation and update
     * @param dto SupplierDto to validate
     * @return error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(SupplierDto dto) {
        StringBuilder missingFields = new StringBuilder();

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            missingFields.append("name, ");
        }

        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            missingFields.append("phone, ");
        }

        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            missingFields.append("email, ");
        }

        if (!missingFields.isEmpty()) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }

}
