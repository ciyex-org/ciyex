package com.qiaben.ciyex.controller;


import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ProviderDto;
import com.qiaben.ciyex.entity.ProviderStatus;
import com.qiaben.ciyex.service.ProviderService;
import lombok.Data;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// NEW import
import com.qiaben.ciyex.dto.ProviderPasswordResetRequest;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    private final ProviderService service;

    public ProviderController(ProviderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProviderDto>> create(@RequestBody ProviderDto dto) {
        try {
            ProviderDto createdProvider = service.create(dto);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(true)
                            .message("Provider created successfully")
                            .data(createdProvider)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Validation failed")
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to create provider")
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProviderDto>> get(@PathVariable Long id) {
        try {
            ProviderDto provider = service.getById(id);
            if (provider == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<ProviderDto>builder()
                                .success(false)
                                .message("Provider not found")
                                .data(null)
                                .build());
            }
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(true)
                            .message("Provider retrieved successfully")
                            .data(provider)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to retrieve provider")
                            .data(null)
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProviderDto>> update(@PathVariable Long id, @RequestBody ProviderDto dto) {
        try {
            ProviderDto updatedProvider = service.update(id, dto);
            if (updatedProvider == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<ProviderDto>builder()
                                .success(false)
                                .message("Provider not found")
                                .data(null)
                                .build());
            }
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(true)
                            .message("Provider updated successfully")
                            .data(updatedProvider)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to update provider")
                            .data(null)
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Provider deleted successfully")
                            .data(null)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Provider not found")
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete provider")
                            .data(null)
                            .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProviderDto>>> getAllProviders() {
        try {
            ApiResponse<List<ProviderDto>> response = service.getAllProviders();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<ProviderDto>>builder()
                            .success(false)
                            .message("Failed to retrieve providers")
                            .data(null)
                            .build());
        }
    }

    // Global exception handler for the controller
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<ProviderDto>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.<ProviderDto>builder()
                        .success(false)
                        .message(ex.getReason())
                        .data(null)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ProviderDto>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<ProviderDto>builder()
                        .success(false)
                        .message("An unexpected error occurred")
                        .data(null)
                        .build());
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getProviderCountByOrgId() {
        try {
            long count = service.getProviderCountByOrgId();
            return ResponseEntity.ok(
                    ApiResponse.<Long>builder()
                            .success(true)
                            .message("Provider count retrieved successfully")
                            .data(count)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Long>builder()
                            .success(false)
                            .message("Failed to retrieve provider count")
                            .data(null)
                            .build());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProviderDto>> updateStatus(
            @PathVariable Long id,
            @RequestBody StatusUpdateRequest request
    ) {
        try {
            ProviderDto updatedProvider = service.updateStatus(id, request.getStatus());
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(true)
                            .message("Provider status updated successfully")
                            .data(updatedProvider)
                            .build()
            );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Provider not found")
                            .data(null)
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to update provider status")
                            .data(null)
                            .build());
        }
    }

    // === NEW: reset provider password by provider id ===
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetProviderPassword(
            @PathVariable Long id,
            @RequestBody ProviderPasswordResetRequest request
    ) {
        try {
            service.resetProviderPassword(id, request.getNewPassword());
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message("Password reset successfully")
                            .data(null)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Void>builder().success(false).message(e.getMessage()).data(null).build()
            );
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(
                    ApiResponse.<Void>builder().success(false).message("Password not changed: " + e.getMostSpecificCause().getMessage()).data(null).build()
            );
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    ApiResponse.<Void>builder().success(false).message(e.getMessage()).data(null).build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Void>builder().success(false).message("Failed to reset password").data(null).build()
            );
        }
    }

    @Data
    public static class StatusUpdateRequest {
        private ProviderStatus status;
    }

}