package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.OrgDto;
import com.qiaben.ciyex.service.OrgService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/orgs")
public class OrgController {

    private final OrgService service;

    public OrgController(OrgService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrgDto>> create(@RequestBody OrgDto dto) {
        try {
            OrgDto createdOrg = service.create(dto);
            return ResponseEntity.ok(
                    ApiResponse.<OrgDto>builder()
                            .success(true)
                            .message("Organization created successfully")
                            .data(createdOrg)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<OrgDto>builder()
                            .success(false)
                            .message("Failed to create organization")
                            .data(null)
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrgDto>> get(@PathVariable Long id) {
        try {
            OrgDto org = service.getById(id);
            if (org == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<OrgDto>builder()
                                .success(false)
                                .message("Organization not found")
                                .data(null)
                                .build());
            }
            return ResponseEntity.ok(
                    ApiResponse.<OrgDto>builder()
                            .success(true)
                            .message("Organization retrieved successfully")
                            .data(org)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<OrgDto>builder()
                            .success(false)
                            .message("Failed to retrieve organization")
                            .data(null)
                            .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrgDto>> update(@PathVariable Long id, @RequestBody OrgDto dto) {
        try {
            OrgDto updatedOrg = service.update(id, dto);
            if (updatedOrg == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<OrgDto>builder()
                                .success(false)
                                .message("Organization not found")
                                .data(null)
                                .build());
            }
            return ResponseEntity.ok(
                    ApiResponse.<OrgDto>builder()
                            .success(true)
                            .message("Organization updated successfully")
                            .data(updatedOrg)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<OrgDto>builder()
                            .success(false)
                            .message("Failed to update organization")
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
                            .message("Organization deleted successfully")
                            .data(null)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete organization")
                            .data(null)
                            .build());
        }
    }

    // Global exception handler for the controller
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<OrgDto>> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.<OrgDto>builder()
                        .success(false)
                        .message(ex.getReason())
                        .data(null)
                        .build());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<OrgDto>> handleSecurityException(SecurityException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.<OrgDto>builder()
            .success(false)
            .message(ex.getMessage())
            .data(null)
            .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<OrgDto>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<OrgDto>builder()
                        .success(false)
                        .message("An unexpected error occurred")
                        .data(null)
                        .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrgDto>>> getAll() {
        try {
            List<OrgDto> orgs = service.getAll();
            return ResponseEntity.ok(
                    ApiResponse.<List<OrgDto>>builder()
                            .success(true)
                            .message("Organizations retrieved successfully")
                            .data(orgs)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<OrgDto>>builder()
                            .success(false)
                            .message("Failed to retrieve organizations")
                            .data(null)
                            .build());
        }
    }

    /**
     * Update only the status of an organization.
     * Expected payload: { "status": "ACTIVE" } or { "status": "INACTIVE" }
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrgDto>> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String status = body != null ? body.get("status") : null;
            if (status == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.<OrgDto>builder()
                                .success(false)
                                .message("Missing 'status' in request body")
                                .data(null)
                                .build());
            }

            // Normalize and validate
            status = status.trim().toUpperCase();
            if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.<OrgDto>builder()
                                .success(false)
                                .message("Invalid status value. Allowed: ACTIVE, INACTIVE")
                                .data(null)
                                .build());
            }

            OrgDto updated = service.updateStatus(id, status);
            if (updated == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<OrgDto>builder()
                                .success(false)
                                .message("Organization not found")
                                .data(null)
                                .build());
            }

            return ResponseEntity.ok(
                    ApiResponse.<OrgDto>builder()
                            .success(true)
                            .message("Organization status updated successfully")
                            .data(updated)
                            .build()
            );
        } catch (ResponseStatusException ex) {
            return handleResponseStatusException(ex);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<OrgDto>builder()
                            .success(false)
                            .message("Failed to update organization status")
                            .data(null)
                            .build());
        }
    }

}