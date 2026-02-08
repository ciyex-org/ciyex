package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.CoverageDto;
import org.ciyex.ehr.service.CoverageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coverages")
@Slf4j
public class CoverageController {

    private final CoverageService service;

    public CoverageController(CoverageService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CoverageDto>> create(
            @Valid @RequestBody CoverageDto dto) {
        try {
            CoverageDto created = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create Coverage: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("Failed to create Coverage: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<CoverageDto>> getByPatient(
            @PathVariable("patientId") Long patientId) {
        try {
            CoverageDto dto = service.getByPatientId(patientId);
            // Check if coverage data is empty or contains only null values
            if (dto == null || (dto.getPatientId() == null && dto.getPolicyNumber() == null)) {
                return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                        .success(false)
                        .message("No coverage found for patient ID: " + patientId)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Coverage for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("No coverage found for patient ID: " + patientId)
                    .build());
        }
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<ApiResponse<CoverageDto>> updateByPatient(
            @PathVariable("patientId") Long patientId,
            @Valid @RequestBody CoverageDto dto) {
        try {
            CoverageDto updated = service.updateByPatientId(patientId, dto);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update Coverage for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("Failed to update Coverage: " + e.getMessage())
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
                    .message("Coverage deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete Coverage for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete Coverage: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{patientId}/{coverageId}")
    public ResponseEntity<ApiResponse<CoverageDto>> getItem(
            @PathVariable("patientId") Long patientId,
            @PathVariable("coverageId") Long coverageId) {
        try {
            CoverageDto item = service.getItem(patientId, coverageId);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage retrieved successfully")
                    .data(item)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve item {} for patientId {}: {}", coverageId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("Failed to retrieve coverage: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{patientId}/{coverageId}")
    public ResponseEntity<ApiResponse<CoverageDto>> updateItem(
            @PathVariable("patientId") Long patientId,
            @PathVariable("coverageId") Long coverageId,
            @Valid @RequestBody CoverageDto dto) {
        try {
            CoverageDto updated = service.updateItem(patientId, coverageId, dto);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update item {} for patientId {}: {}", coverageId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("Failed to update coverage: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{patientId}/{coverageId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable("patientId") Long patientId,
            @PathVariable("coverageId") Long coverageId) {
        try {
            service.deleteItem(patientId, coverageId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Coverage deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete item {} for patientId {}: {}", coverageId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete coverage: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CoverageDto>>> searchAll() {
        try {
            ApiResponse<List<CoverageDto>> res = service.searchAll();
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Failed to search all Coverages: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<CoverageDto>>builder()
                    .success(false)
                    .message("Failed to retrieve coverages: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }
}