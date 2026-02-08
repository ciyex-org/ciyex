package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.ImmunizationDto;
import org.ciyex.ehr.service.ImmunizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/immunizations")
@Slf4j
public class ImmunizationController {

    private final ImmunizationService service;

    public ImmunizationController(ImmunizationService service) {
        this.service = service;
    }

    // ---------- Patient-level endpoints ----------

    @PostMapping
    public ResponseEntity<ApiResponse<ImmunizationDto>> create(
            @RequestBody ImmunizationDto dto) {
        // orgId header deprecated; tenant isolation handled via RequestContext (tenantName) set upstream
        try {
            ImmunizationDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(true)
                    .message("Immunization created successfully")
                    .data(created)
                    .build());
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating Immunization: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(false)
                    .message("Failed to create immunization: " + e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create Immunization", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(false)
                    .message("Failed to create immunization: " + errorMsg)
                    .data(null)
                    .build());
        }
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<ImmunizationDto>> getByPatient(
            @PathVariable("patientId") Long patientId) {
        try {
            ImmunizationDto dto = service.getByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(true)
                    .message("Immunizations retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Immunization for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(false)
                    .message("Failed to retrieve immunization: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<ApiResponse<Void>> deleteByPatient(
            @PathVariable("patientId") Long patientId) {
        try {
            service.deleteByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Immunization deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete Immunization for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete immunization: " + e.getMessage())
                    .build());
        }
    }

    // ---------- Item-level endpoints (/{patientId}/{immunizationId}) ----------

    @GetMapping("/{patientId}/{immunizationId}")
    public ResponseEntity<ApiResponse<ImmunizationDto.ImmunizationItem>> getItem(
            @PathVariable("patientId") Long patientId,
            @PathVariable("immunizationId") Long immunizationId) {
        try {
            var item = service.getItem(patientId, immunizationId);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto.ImmunizationItem>builder()
                    .success(true)
                    .message("Immunization item retrieved successfully")
                    .data(item)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve item {} for patientId {}: {}", immunizationId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto.ImmunizationItem>builder()
                    .success(false)
                    .message("Failed to retrieve immunization: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @PutMapping("/{patientId}/{immunizationId}")
    public ResponseEntity<ApiResponse<ImmunizationDto.ImmunizationItem>> updateItem(
            @PathVariable("patientId") Long patientId,
            @PathVariable("immunizationId") Long immunizationId,
            @RequestBody ImmunizationDto.ImmunizationItem item) {
        try {
            var updated = service.updateItem(patientId, immunizationId, item);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto.ImmunizationItem>builder()
                    .success(true)
                    .message("Immunization item updated successfully")
                    .data(updated)
                    .build());
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating item {} for patientId {}: {}", immunizationId, patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto.ImmunizationItem>builder()
                    .success(false)
                    .message("Failed to update immunization: " + e.getMessage())
                    .data(null)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update item {} for patientId {}: {}", immunizationId, patientId, e.getMessage(), e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto.ImmunizationItem>builder()
                    .success(false)
                    .message("Failed to update immunization: " + errorMsg)
                    .data(null)
                    .build());
        }
    }

    @DeleteMapping("/{patientId}/{immunizationId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable("patientId") Long patientId,
            @PathVariable("immunizationId") Long immunizationId) {
        try {
            service.deleteItem(patientId, immunizationId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Immunization item deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete item {} for patientId {}: {}", immunizationId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete immunization: " + e.getMessage())
                    .build());
        }
    }

    // ---------- Search All ----------
    @GetMapping
    public ResponseEntity<ApiResponse<List<ImmunizationDto>>> searchAll() {
        try {
            ApiResponse<List<ImmunizationDto>> res = service.searchAll();
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Failed to search all Immunizations: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<ImmunizationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve immunizations: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }
}
