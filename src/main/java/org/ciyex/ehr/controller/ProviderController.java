package org.ciyex.ehr.controller;


import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.ProviderDto;
import org.ciyex.ehr.dto.ProviderStatus;
import org.ciyex.ehr.service.ProviderService;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// NEW import
import org.ciyex.ehr.dto.ProviderPasswordResetRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/providers")
@Slf4j
public class ProviderController {

    private final ProviderService service;

    public ProviderController(ProviderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProviderDto>> create(@Valid @RequestBody ProviderDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(
                        ApiResponse.<ProviderDto>builder()
                                .success(false)
                                .message("Validation failed")
                                .data(null)
                                .build());
            }
            ProviderDto createdProvider = service.create(dto);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(true)
                            .message("Provider created successfully")
                            .data(createdProvider)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.error("Failed to create Provider: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to create provider: " + e.getMessage())
                            .data(null)
                            .build());
        } catch (Exception e) {
            log.error("Failed to create Provider: {}", e.getMessage(), e);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to create provider: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProviderDto>> get(@PathVariable("id") Long id) {
        try {
            ProviderDto provider = service.getById(id);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(true)
                            .message("Provider retrieved successfully")
                            .data(provider)
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Failed to retrieve Provider with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to retrieve provider: " + e.getMessage())
                            .data(null)
                            .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Provider with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to retrieve provider: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProviderDto>> update(@PathVariable("id") Long id, @Valid @RequestBody ProviderDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                Map<String, String> errors = new HashMap<>();
                result.getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
                return ResponseEntity.badRequest().body(
                        ApiResponse.<ProviderDto>builder()
                                .success(false)
                                .message("Validation failed")
                                .data(null)
                                .build());
            }
            ProviderDto updatedProvider = service.update(id, dto);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(true)
                            .message("Provider updated successfully")
                            .data(updatedProvider)
                            .build()
            );
        } catch (RuntimeException e) {
            log.error("Failed to update Provider with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to update provider: " + e.getMessage())
                            .data(null)
                            .build());
        } catch (Exception e) {
            log.error("Failed to update Provider with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to update provider: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
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
            log.error("Failed to delete Provider with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Provider not found")
                            .data(null)
                            .build());
        } catch (Exception e) {
            log.error("Failed to delete Provider with id {}: {}", id, e.getMessage(), e);
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

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getProviderCountBy() {
        try {
            long count = service.getProviderCount();
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
            @PathVariable("id") Long id,
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
            log.error("Failed to update Provider status with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to update provider status: " + e.getMessage())
                            .data(null)
                            .build());
        } catch (Exception e) {
            log.error("Failed to update Provider status with id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(
                    ApiResponse.<ProviderDto>builder()
                            .success(false)
                            .message("Failed to update provider status: " + e.getMessage())
                            .data(null)
                            .build());
        }
    }

    // === NEW: reset provider password by provider id ===
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetProviderPassword(
            @PathVariable("id") Long id,
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
