package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MedicalProblemDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.MedicalProblemService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            @Valid @RequestBody MedicalProblemDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getAllErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                        .success(false)
                        .message(errorMsg.toString())
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
            @PathVariable("patientId") Long patientId) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated upstream.
            RequestContext.set(ctx);

            MedicalProblemDto dto = service.getByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(true).message("Medical Problem retrieved successfully").data(dto).build());
        } catch (Exception e) {
            log.error("Failed to retrieve Medical Problem for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(false).message("Failed to retrieve Medical Problem: " + e.getMessage()).build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<ApiResponse<MedicalProblemDto>> updateByPatient(
            @PathVariable("patientId") Long patientId,
            @Valid @RequestBody MedicalProblemDto dto, BindingResult result) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getAllErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                        .success(false)
                        .message(errorMsg.toString())
                        .build());
            }

            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated upstream.
            RequestContext.set(ctx);

            MedicalProblemDto updated = service.updateByPatientId(patientId, dto);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(true).message("Medical Problem updated successfully").data(updated).build());
        } catch (Exception e) {
            log.error("Failed to update Medical Problem for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto>builder()
                    .success(false).message("Failed to update Medical Problem: " + e.getMessage()).build());
        } finally {
            RequestContext.clear();
        }
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<ApiResponse<Void>> deleteByPatient(
            @PathVariable("patientId") Long patientId) {
        try {
            RequestContext ctx = new RequestContext();
            // orgId deprecated; tenantName populated upstream.
            RequestContext.set(ctx);

            service.deleteByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Medical Problem deleted successfully").build());
        } catch (Exception e) {
            log.error("Failed to delete Medical Problem for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false).message("Failed to delete Medical Problem: " + e.getMessage()).build());
        } finally {
            RequestContext.clear();
        }
    }

    // Item endpoints
    @GetMapping("/{patientId}/{problemId}")
    public ResponseEntity<ApiResponse<MedicalProblemDto.MedicalProblemItem>> getItem(
            @PathVariable("patientId") Long patientId, @PathVariable("problemId") Long problemId) {
        try {
            RequestContext ctx = new RequestContext(); /* orgId deprecated */ RequestContext.set(ctx);
            var item = service.getItem(patientId, problemId);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                    .success(true).message("Medical Problem retrieved successfully").data(item).build());
        } catch (Exception e) {
            log.error("Failed to retrieve Medical Problem item {} for patientId {}: {}", problemId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                    .success(false).message("Failed to retrieve medical problem: " + e.getMessage()).build());
        } finally { RequestContext.clear(); }
    }

    @PutMapping("/{patientId}/{problemId}")
    public ResponseEntity<ApiResponse<MedicalProblemDto.MedicalProblemItem>> updateItem(
            @PathVariable("patientId") Long patientId, @PathVariable("problemId") Long problemId,
            @Valid @RequestBody MedicalProblemDto.MedicalProblemItem patch, BindingResult result) {
        try {
            if (result.hasErrors()) {
                StringBuilder errorMsg = new StringBuilder();
                result.getAllErrors().forEach(error -> 
                    errorMsg.append(error.getDefaultMessage()).append(", ")
                );
                if (errorMsg.length() > 0) {
                    errorMsg.setLength(errorMsg.length() - 2);
                }
                return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                        .success(false)
                        .message(errorMsg.toString())
                        .build());
            }

            RequestContext ctx = new RequestContext(); /* orgId deprecated */ RequestContext.set(ctx);
            var updated = service.updateItem(patientId, problemId, patch);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                    .success(true).message("Medical Problem updated successfully").data(updated).build());
        } catch (Exception e) {
            log.error("Failed to update Medical Problem item {} for patientId {}: {}", problemId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<MedicalProblemDto.MedicalProblemItem>builder()
                    .success(false).message("Failed to update medical problem: " + e.getMessage()).build());
        } finally { RequestContext.clear(); }
    }

    @DeleteMapping("/{patientId}/{problemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable("patientId") Long patientId, @PathVariable("problemId") Long problemId) {
        try {
            RequestContext ctx = new RequestContext(); /* orgId deprecated */ RequestContext.set(ctx);
            service.deleteItem(patientId, problemId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message("Medical Problem deleted successfully").build());
        } catch (Exception e) {
            log.error("Failed to delete Medical Problem item {} for patientId {}: {}", problemId, patientId, e.getMessage(), e);
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
}
