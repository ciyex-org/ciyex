package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.ImmunizationService;
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
            @RequestBody ImmunizationDto dto,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            ImmunizationDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(true)
                    .message("Immunization created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create Immunization: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(false)
                    .message("Failed to create Immunization: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<ImmunizationDto>> getByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            ImmunizationDto dto = service.getByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(true)
                    .message("Immunization retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve Immunization for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(false)
                    .message("Failed to retrieve Immunization: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{patientId}")
    public ResponseEntity<ApiResponse<ImmunizationDto>> updateByPatient(
            @PathVariable Long patientId,
            @RequestBody ImmunizationDto dto,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            ImmunizationDto updated = service.updateByPatientId(patientId, dto);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(true)
                    .message("Immunization updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update Immunization for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto>builder()
                    .success(false)
                    .message("Failed to update Immunization: " + e.getMessage())
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
                    .message("Immunization deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete Immunization for patientId {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete Immunization: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---------- Item-level endpoints (/{patientId}/{immunizationId}) ----------

    @GetMapping("/{patientId}/{immunizationId}")
    public ResponseEntity<ApiResponse<ImmunizationDto.ImmunizationItem>> getItem(
            @PathVariable Long patientId,
            @PathVariable Long immunizationId,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

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
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{patientId}/{immunizationId}")
    public ResponseEntity<ApiResponse<ImmunizationDto.ImmunizationItem>> updateItem(
            @PathVariable Long patientId,
            @PathVariable Long immunizationId,
            @RequestBody ImmunizationDto.ImmunizationItem patch,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            var updated = service.updateItem(patientId, immunizationId, patch);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto.ImmunizationItem>builder()
                    .success(true)
                    .message("Immunization item updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update item {} for patientId {}: {}", immunizationId, patientId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<ImmunizationDto.ImmunizationItem>builder()
                    .success(false)
                    .message("Failed to update immunization: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @DeleteMapping("/{patientId}/{immunizationId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable Long patientId,
            @PathVariable Long immunizationId,
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

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
        } finally {
            RequestContext.clear();
        }
    }

    // ---------- Search All ----------
    @GetMapping
    public ResponseEntity<ApiResponse<List<ImmunizationDto>>> searchAll(
            @RequestHeader("orgId") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            ApiResponse<List<ImmunizationDto>> res = service.searchAll();
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Failed to search all Immunizations: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<List<ImmunizationDto>>builder()
                    .success(false)
                    .message("Failed to retrieve Immunizations: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }
}
