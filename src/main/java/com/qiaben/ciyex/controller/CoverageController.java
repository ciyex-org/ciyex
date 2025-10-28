package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CoverageDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.CoverageService;
import lombok.extern.slf4j.Slf4j;
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
            @RequestBody CoverageDto dto) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            // orgId removed from DTO; tenant inferred from RequestContext.

            CoverageDto createdCoverage = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage created successfully")
                    .data(createdCoverage)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create coverage: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("Failed to create coverage: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CoverageDto>> get(
            @PathVariable Long id) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            CoverageDto coverage = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage retrieved successfully")
                    .data(coverage)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve coverage with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("Failed to retrieve coverage: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CoverageDto>> update(
            @PathVariable Long id,
            @RequestBody CoverageDto dto) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            // orgId removed from DTO; tenant inferred from RequestContext.

            CoverageDto updatedCoverage = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage updated successfully")
                    .data(updatedCoverage)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update coverage with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("Failed to update coverage: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Coverage deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete coverage with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete coverage: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }


    // ---- NEW: Composite (id + patientId) endpoints ----
    @GetMapping("/{id}/{patientId}")
    public ResponseEntity<ApiResponse<CoverageDto>> getByIdAndPatient(
            @PathVariable Long id,
            @PathVariable Long patientId) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            CoverageDto dto = service.getByIdAndPatientId(id, patientId);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve coverage id {} for patientId {}: {}", id, patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("Failed to retrieve coverage: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{id}/{patientId}")
    public ResponseEntity<ApiResponse<CoverageDto>> updateByIdAndPatient(
            @PathVariable Long id,
            @PathVariable Long patientId,
            @RequestBody CoverageDto dto) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            // orgId removed from DTO; tenant inferred from RequestContext.
            CoverageDto updated = service.updateByIdAndPatientId(id, patientId, dto);
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(true)
                    .message("Coverage updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update coverage id {} for patientId {}: {}", id, patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<CoverageDto>builder()
                    .success(false)
                    .message("Failed to update coverage: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @DeleteMapping("/{id}/{patientId}")
    public ResponseEntity<ApiResponse<Void>> deleteByIdAndPatient(
            @PathVariable Long id,
            @PathVariable Long patientId) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            service.deleteByIdAndPatientId(id, patientId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Coverage deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete coverage id {} for patientId {}: {}", id, patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete coverage: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CoverageDto>>> getAllCoverages() {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            List<CoverageDto> coverages = service.getAllCoverages();
            return ResponseEntity.ok(ApiResponse.<List<CoverageDto>>builder()
                    .success(true)
                    .message("Coverages retrieved successfully")
                    .data(coverages)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve all coverages: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<CoverageDto>>builder()
                    .success(false)
                    .message("Failed to retrieve coverages: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }


}
