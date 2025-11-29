package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CoverageDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.CoverageService;
import com.qiaben.ciyex.service.InsuranceCardUploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping({"/api/coverages", "/api/fhir/insurance", "/api/insurance"})
@Slf4j
public class CoverageController {

    private final CoverageService service;
    private final InsuranceCardUploadService cardUploadService;

    public CoverageController(CoverageService service, InsuranceCardUploadService cardUploadService) {
        this.service = service;
        this.cardUploadService = cardUploadService;
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

    // 👩‍⚕️ Patient Portal Endpoint - Only logged-in patient can see their insurance coverage
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<CoverageDto>> getMyInsurance(org.springframework.security.core.Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ApiResponse.<List<CoverageDto>>builder()
                        .success(false)
                        .message("Unauthorized - not authenticated")
                        .data(null)
                        .build();
            }

            // Delegate to service which understands portal Authentication principals
            List<CoverageDto> coverages = service.getCoveragesForPortalUser(authentication);

            return ApiResponse.<List<CoverageDto>>builder()
                    .success(true)
                    .message("Patient insurance coverage retrieved")
                    .data(coverages)
                    .build();

        } catch (Exception e) {
            log.error("Error getting insurance coverage for portal user: {}", e.getMessage(), e);
            return ApiResponse.<List<CoverageDto>>builder()
                    .success(false)
                    .message("Error retrieving insurance coverage: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }

    /**
     * Upload insurance card front image
     */
    @PostMapping("/{id}/card/front")
    public ResponseEntity<ApiResponse<String>> uploadCardFront(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            String url = cardUploadService.uploadCard(file, id, "front");
            service.updateCardUrl(id, url, true);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Card front uploaded successfully")
                    .data(url)
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload card front for coverage {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to upload card: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /**
     * Upload insurance card back image
     */
    @PostMapping("/{id}/card/back")
    public ResponseEntity<ApiResponse<String>> uploadCardBack(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            RequestContext ctx = new RequestContext();
            RequestContext.set(ctx);

            String url = cardUploadService.uploadCard(file, id, "back");
            service.updateCardUrl(id, url, false);

            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Card back uploaded successfully")
                    .data(url)
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload card back for coverage {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(false)
                    .message("Failed to upload card: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

}
