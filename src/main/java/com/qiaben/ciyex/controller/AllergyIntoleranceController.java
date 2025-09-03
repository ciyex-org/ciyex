// src/main/java/com/qiaben/ciyex/controller/AllergyIntoleranceController.java
package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.AllergyIntoleranceDto;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
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
            @RequestBody AllergyIntoleranceDto dto,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

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
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<AllergyIntoleranceDto>> getByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            AllergyIntoleranceDto dto = service.getByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto>builder()
                    .success(true)
                    .message("Allergy Intolerance retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Allergy Intolerance for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<AllergyIntoleranceDto>builder()
                    .success(false)
                    .message("Failed to retrieve Allergy Intolerance: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<ApiResponse<AllergyIntoleranceDto>> updateByPatient(
            @PathVariable Long patientId,
            @RequestBody AllergyIntoleranceDto dto,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

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
        } finally {
            RequestContext.clear();
        }
    }

    @DeleteMapping("/{patientId}")
    public ResponseEntity<ApiResponse<Void>> deleteByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

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
        } finally {
            RequestContext.clear();
        }
    }

    // ---------- Item endpoints (now simplified to /{patientId}/{intoleranceId}) ----------

    @GetMapping("/{patientId}/{intoleranceId}")
    public ResponseEntity<ApiResponse<AllergyIntoleranceDto.AllergyItem>> getItem(
            @PathVariable Long patientId,
            @PathVariable Long intoleranceId,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

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
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{patientId}/{intoleranceId}")
    public ResponseEntity<ApiResponse<AllergyIntoleranceDto.AllergyItem>> updateItem(
            @PathVariable Long patientId,
            @PathVariable Long intoleranceId,
            @RequestBody AllergyIntoleranceDto.AllergyItem patch,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            var updated = service.updateItem(patientId, intoleranceId, patch);
            AllergyIntoleranceDto dto = service.getByPatientId(patientId); // fetch full DT
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
        } finally {
            RequestContext.clear();
        }
    }

    @DeleteMapping("/{patientId}/{intoleranceId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long patientId,
            @PathVariable Long intoleranceId,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

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
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AllergyIntoleranceDto>>> searchAll(
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            ApiResponse<List<AllergyIntoleranceDto>> res = service.searchAll();
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Failed to search all Allergy Intolerances: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<AllergyIntoleranceDto>>builder()
                    .success(false)
                    .message("Failed to retrieve Allergy Intolerances: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }
}
