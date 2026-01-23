package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SupplierDto;
import com.qiaben.ciyex.service.SupplierService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/suppliers")
@Slf4j
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierDto>> create(@Valid @RequestBody SupplierDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getFieldErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                        .success(false)
                        .message(errorMsg.toString())
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
            SupplierDto data = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(true)
                    .message("Supplier retrieved successfully")
                    .data(data)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Supplier with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(false)
                    .message("Supplier not found with ID: " + id)
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierDto>> update(@PathVariable("id") String id, @Valid @RequestBody SupplierDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getFieldErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                        .success(false)
                        .message(errorMsg.toString())
                        .build());
            }

            SupplierDto data = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(true)
                    .message("Supplier updated successfully")
                    .data(data)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update Supplier with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<SupplierDto>builder()
                    .success(false)
                    .message("Supplier not found with ID: " + id)
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
                    .message("Supplier not found with ID: " + id)
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SupplierDto>>> getAll(
            @PageableDefault(size = 10, page = 0) Pageable pageable) {
        try {
            return ResponseEntity.ok(ApiResponse.<Page<SupplierDto>>builder()
                    .success(true)
                    .message("Suppliers retrieved successfully")
                    .data(service.getAll(pageable))
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve suppliers: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Page<SupplierDto>>builder()
                    .success(false)
                    .message("Failed to retrieve suppliers: " + e.getMessage())
                    .build());
        }
    }
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .success(true)
                .message("Supplier count retrieved successfully")
                .data(service.countByOrg())
                .build());
    }
}
