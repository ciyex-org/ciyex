package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MedicalProblemDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.MedicalProblemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-problems")
@Slf4j
public class MedicalProblemController {

    private final MedicalProblemService service;

    public MedicalProblemController(MedicalProblemService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MedicalProblemDto>> create(
            @RequestBody MedicalProblemDto dto) {
        try {
            // ✅ Validate mandatory fields before processing
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                        .success(false)
                        .message(validationError)
                        .build());
            }

            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated upstream.
            RequestContext.set(ctx);

            MedicalProblemDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(true).message("Medical Problem created successfully").data(created).build());
        } catch (Exception e) {
            log.error("Failed to create Medical Problem: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(false).message("Failed to create Medical Problem: " + e.getMessage()).build());
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<MedicalProblemDto>> getByPatient(
            @PathVariable Long patientId) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated upstream.
            RequestContext.set(ctx);

            MedicalProblemDto dto = service.getByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(true).message("Medical Problem retrieved successfully").data(dto).build());
        } catch (Exception e) {
            log.error("Failed to retrieve Medical Problem: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(false).message("Failed to retrieve Medical Problem: " + e.getMessage()).build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<ApiResponse<MedicalProblemDto>> updateByPatient(
            @PathVariable Long patientId,
            @RequestBody MedicalProblemDto dto) {
        try {
            // ✅ Validate mandatory fields before processing
            String validationError = validateMandatoryFields(dto);
            if (validationError != null) {
                return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                        .success(false)
                        .message(validationError)
                        .build());
            }

            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated upstream.
            RequestContext.set(ctx);

            MedicalProblemDto updated = service.updateByPatientId(patientId, dto);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(true).message("Medical Problem updated successfully").data(updated).build());
        } catch (Exception e) {
            log.error("Failed to update Medical Problem: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(false).message("Failed to update Medical Problem: " + e.getMessage()).build());
        } finally {
            RequestContext.clear();
        }
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<ApiResponse<Void>> deleteByPatient(
            @PathVariable Long patientId) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated upstream.
            RequestContext.set(ctx);

            service.deleteByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Medical Problem deleted successfully").build());
        } catch (Exception e) {
            log.error("Failed to delete Medical Problem: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("Failed to delete Medical Problem: " + e.getMessage()).build());
        } finally {
            RequestContext.clear();
        }
    }

    // Item endpoints
    @GetMapping("/{patientId}/{problemId}")
    public ResponseEntity<ApiResponse<MedicalProblemDto.MedicalProblemItem>> getItem(
            @PathVariable Long patientId, @PathVariable Long problemId) {
        try {
            RequestContext ctx = new RequestContext(); /* orgId deprecated */ RequestContext.set(ctx);
            var item = service.getItem(patientId, problemId);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                    .success(true).message("Medical Problem retrieved successfully").data(item).build());
        } catch (Exception e) {
            log.error("Failed to retrieve item: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                    .success(false).message("Failed to retrieve medical problem: " + e.getMessage()).build());
        } finally { RequestContext.clear(); }
    }

    @PutMapping("/{patientId}/{problemId}")
    public ResponseEntity<ApiResponse<MedicalProblemDto.MedicalProblemItem>> updateItem(
            @PathVariable Long patientId, @PathVariable Long problemId,
            @RequestBody MedicalProblemDto.MedicalProblemItem patch) {
        try {
            // ✅ Validate mandatory fields for the individual item
            String validationError = validateMandatoryFields(patch);
            if (validationError != null) {
                return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                        .success(false)
                        .message(validationError)
                        .build());
            }

            RequestContext ctx = new RequestContext(); /* orgId deprecated */ RequestContext.set(ctx);
            var updated = service.updateItem(patientId, problemId, patch);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                    .success(true).message("Medical Problem updated successfully").data(updated).build());
        } catch (Exception e) {
            log.error("Failed to update item: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                    .success(false).message("Failed to update medical problem: " + e.getMessage()).build());
        } finally { RequestContext.clear(); }
    }

    @DeleteMapping("/{patientId}/{problemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long patientId, @PathVariable Long problemId) {
        try {
            RequestContext ctx = new RequestContext(); /* orgId deprecated */ RequestContext.set(ctx);
            service.deleteItem(patientId, problemId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Medical Problem deleted successfully").build());
        } catch (Exception e) {
            log.error("Failed to delete item: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("Failed to delete medical problem: " + e.getMessage()).build());
        } finally { RequestContext.clear(); }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicalProblemDto>>> searchAll() {
        try {
            RequestContext ctx = new RequestContext(); /* orgId deprecated */ RequestContext.set(ctx);
            return ResponseEntity.ok(service.searchAll());
        } catch (Exception e) {
            log.error("Failed to search all Medical Problems: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<MedicalProblemDto>>builder()
                    .success(false).message("Failed to retrieve Medical Problems: " + e.getMessage()).build());
        } finally { RequestContext.clear(); }
    }

    /**
     * Validates mandatory fields for Medical Problem items.
     * Required fields: title, outcome, verificationStatus, occurrence
     *
     * @param dto The MedicalProblemDto to validate
     * @return Error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(MedicalProblemDto dto) {
        if (dto.getProblemsList() == null || dto.getProblemsList().isEmpty()) {
            return "At least one medical problem item is required";
        }

        StringBuilder missingFields = new StringBuilder();

        for (int i = 0; i < dto.getProblemsList().size(); i++) {
            MedicalProblemDto.MedicalProblemItem item = dto.getProblemsList().get(i);
            StringBuilder itemErrors = new StringBuilder();

            if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
                itemErrors.append("title, ");
            }

            if (item.getOutcome() == null || item.getOutcome().trim().isEmpty()) {
                itemErrors.append("outcome, ");
            }

            if (item.getVerificationStatus() == null || item.getVerificationStatus().trim().isEmpty()) {
                itemErrors.append("verificationStatus, ");
            }

            if (item.getOccurrence() == null || item.getOccurrence().trim().isEmpty()) {
                itemErrors.append("occurrence, ");
            }

            if (itemErrors.length() > 0) {
                // Remove the trailing comma and space
                itemErrors.setLength(itemErrors.length() - 2);
                missingFields.append("Item ").append(i + 1).append(" missing: ").append(itemErrors).append("; ");
            }
        }

        if (missingFields.length() > 0) {
            // Remove the trailing semicolon and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }

    /**
     * Validates mandatory fields for a single Medical Problem item.
     * Required fields: title, outcome, verificationStatus, occurrence
     *
     * @param item The MedicalProblemItem to validate
     * @return Error message if validation fails, null if validation passes
     */
    private String validateMandatoryFields(MedicalProblemDto.MedicalProblemItem item) {
        StringBuilder missingFields = new StringBuilder();

        if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
            missingFields.append("title, ");
        }

        if (item.getOutcome() == null || item.getOutcome().trim().isEmpty()) {
            missingFields.append("outcome, ");
        }

        if (item.getVerificationStatus() == null || item.getVerificationStatus().trim().isEmpty()) {
            missingFields.append("verificationStatus, ");
        }

        if (item.getOccurrence() == null || item.getOccurrence().trim().isEmpty()) {
            missingFields.append("occurrence, ");
        }

        if (missingFields.length() > 0) {
            // Remove the trailing comma and space
            missingFields.setLength(missingFields.length() - 2);
            return "Missing mandatory fields: " + missingFields;
        }

        return null;
    }
}
