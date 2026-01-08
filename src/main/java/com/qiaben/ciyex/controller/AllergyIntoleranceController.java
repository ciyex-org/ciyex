package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.AllergyIntoleranceDto;
// Removed orgId usage; RequestContext (tenantName) is set upstream if needed
import com.qiaben.ciyex.service.AllergyIntoleranceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allergy-intolerances")
@Slf4j
public class AllergyIntoleranceController {

    private final AllergyIntoleranceService service;

    public AllergyIntoleranceController(AllergyIntoleranceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AllergyIntoleranceDto>> create(
            @RequestBody AllergyIntoleranceDto dto) {
        try {
            AllergyIntoleranceDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto>builder()
                    .success(true)
                    .message("Allergy Intolerance created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create Allergy Intolerance: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto>builder()
                    .success(false)
                    .message("Failed to create Allergy Intolerance: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<AllergyIntoleranceDto>> getByPatient(
            @PathVariable("patientId") Long patientId) {
        try {
            AllergyIntoleranceDto dto = service.getByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto>builder()
                    .success(true)
                    .message("Allergy Intolerance retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Allergy Intolerance for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto>builder()
                    .success(true)
                    .message("No allergies found for patient ID: " + patientId)
                    .data(new AllergyIntoleranceDto())
                    .build());
        }
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<ApiResponse<AllergyIntoleranceDto>> updateByPatient(
            @PathVariable("patientId") Long patientId,
            @RequestBody AllergyIntoleranceDto dto) {
        try {
            AllergyIntoleranceDto updated = service.updateByPatientId(patientId, dto);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto>builder()
                    .success(true)
                    .message("Allergy Intolerance updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update Allergy Intolerance for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto>builder()
                    .success(false)
                    .message("Failed to update Allergy Intolerance: " + e.getMessage())
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
                    .message("Allergy Intolerance deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete Allergy Intolerance for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete Allergy Intolerance: " + e.getMessage())
                    .build());
        }
    }

    // ---------- Item endpoints (/{patientId}/{intoleranceId}) ----------

    @GetMapping("/{patientId}/{intoleranceId}")
    public ResponseEntity<ApiResponse<AllergyIntoleranceDto.AllergyItem>> getItem(
            @PathVariable("patientId") Long patientId,
            @PathVariable("intoleranceId") Long intoleranceId) {
        try {
            var item = service.getItem(patientId, intoleranceId);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto.AllergyItem>builder()
                    .success(true)
                    .message("Allergy retrieved successfully")
                    .data(item)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve item {} for patientId {}: {}", intoleranceId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto.AllergyItem>builder()
                    .success(false)
                    .message("Failed to retrieve allergy: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{patientId}/{intoleranceId}")
    public ResponseEntity<ApiResponse<AllergyIntoleranceDto.AllergyItem>> updateItem(
            @PathVariable("patientId") Long patientId,
            @PathVariable("intoleranceId") Long intoleranceId,
            @RequestBody AllergyIntoleranceDto dto) {
        try {
            // Extract the first allergy item from the allergiesList
            AllergyIntoleranceDto.AllergyItem patch = null;
            if (dto.getAllergiesList() != null && !dto.getAllergiesList().isEmpty()) {
                patch = dto.getAllergiesList().get(0);
            } else {
                throw new IllegalArgumentException("allergiesList is required and must contain at least one item");
            }
            
            var updated = service.updateItem(patientId, intoleranceId, patch);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto.AllergyItem>builder()
                    .success(true)
                    .message("Allergy updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update item {} for patientId {}: {}", intoleranceId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto.AllergyItem>builder()
                    .success(false)
                    .message("Failed to update allergy: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{patientId}/{intoleranceId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable("patientId") Long patientId,
            @PathVariable("intoleranceId") Long intoleranceId) {
        try {
            service.deleteItem(patientId, intoleranceId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Allergy deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete item {} for patientId {}: {}", intoleranceId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete allergy: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AllergyIntoleranceDto>>> searchAll() {
        try {
            ApiResponse<List<AllergyIntoleranceDto>> res = service.searchAll();
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Failed to search all Allergy Intolerances: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<AllergyIntoleranceDto>>builder()
                    .success(false)
                    .message("Failed to retrieve Allergy Intolerances: " + e.getMessage())
                    .build());
        }
    }

}
