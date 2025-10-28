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
}
