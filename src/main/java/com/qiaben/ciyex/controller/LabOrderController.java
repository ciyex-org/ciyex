package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.LabOrderDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.LabOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/lab-order")
@RequiredArgsConstructor
@Slf4j
public class LabOrderController {

    private final LabOrderService service;

        // ---------- SEARCH across org (orderNumber / test codes & display / physician, ordering provider,
        // status, priority, diagnosis/procedure codes, order/lab names, result text, patientId exact) ----------
        @GetMapping("/search")
        public ResponseEntity<ApiResponse<List<LabOrderDto>>> search(
                        @RequestParam("q") String q) {
                try {
                                RequestContext ctx = new RequestContext();
                                // orgId deprecated; tenantName populated by interceptor.
                                RequestContext.set(ctx);

                        String qq = (q == null ? "" : q).trim().toLowerCase();

                        var all = service.getAll(); // org-scoped
                        var filtered = (all == null ? List.<LabOrderDto>of() : all).stream()
                                        .filter(d -> {
                                                String orderNumber      = String.valueOf(d.getOrderNumber() == null ? "" : d.getOrderNumber()).toLowerCase();
                                                String testCode         = String.valueOf(d.getTestCode() == null ? "" : d.getTestCode()).toLowerCase();
                                                String testDisplay      = String.valueOf(d.getTestDisplay() == null ? "" : d.getTestDisplay()).toLowerCase();
                                                String physicianName    = String.valueOf(d.getPhysicianName() == null ? "" : d.getPhysicianName()).toLowerCase();
                                                String orderingProvider = String.valueOf(d.getOrderingProvider() == null ? "" : d.getOrderingProvider()).toLowerCase();
                                                String status           = String.valueOf(d.getStatus() == null ? "" : d.getStatus()).toLowerCase();
                                                String priority         = String.valueOf(d.getPriority() == null ? "" : d.getPriority()).toLowerCase();
                                                String diagnosisCode    = String.valueOf(d.getDiagnosisCode() == null ? "" : d.getDiagnosisCode()).toLowerCase();
                                                String procedureCode    = String.valueOf(d.getProcedureCode() == null ? "" : d.getProcedureCode()).toLowerCase();
                                                String orderName        = String.valueOf(d.getOrderName() == null ? "" : d.getOrderName()).toLowerCase();
                                                String labName          = String.valueOf(d.getLabName() == null ? "" : d.getLabName()).toLowerCase();
                                                String result           = String.valueOf(d.getResult() == null ? "" : d.getResult()).toLowerCase();
                                                String patientIdStr     = String.valueOf(d.getPatientId() == null ? "" : d.getPatientId()).toLowerCase();
                                                return orderNumber.contains(qq)
                                                                || testCode.contains(qq)
                                                                || testDisplay.contains(qq)
                                                                || physicianName.contains(qq)
                                                                || orderingProvider.contains(qq)
                                                                || status.contains(qq)
                                                                || priority.contains(qq)
                                                                || diagnosisCode.contains(qq)
                                                                || procedureCode.contains(qq)
                                                                || orderName.contains(qq)
                                                                || labName.contains(qq)
                                                                || result.contains(qq)
                                                                || patientIdStr.equals(qq);
                                        })
                                        .collect(Collectors.toList());
                        return ResponseEntity.ok(ApiResponse.<List<LabOrderDto>>builder()
                                        . success(true)
                                        .message("Lab orders search results")
                                        .data(filtered)
                                        .build());
                } catch (Exception e) {
                        log.error("Failed to search lab orders: {}", e.getMessage(), e);
                        return ResponseEntity.ok(ApiResponse.<List<LabOrderDto>>builder()
                                        .success(false)
                                        .message("Failed to search lab orders: " + e.getMessage())
                                        .build());
                } finally {
                        RequestContext.clear();
                }
    }

        // ---------- READ ALL for patient ----------
        @GetMapping("/{patientId}")
        public ResponseEntity<ApiResponse<List<LabOrderDto>>> listForPatient(
                        @PathVariable Long patientId) {
                try {
                                RequestContext ctx = new RequestContext();
                                // orgId deprecated; tenantName populated by interceptor.
                                RequestContext.set(ctx);
                        var all = service.getAll();
                        var filtered = (all == null ? List.<LabOrderDto>of() : all).stream()
                                        .filter(d -> d.getPatientId() != null && Objects.equals(d.getPatientId(), patientId))
                                        .collect(Collectors.toList());
                        return ResponseEntity.ok(ApiResponse.<List<LabOrderDto>>builder()
                                        .success(true)
                                        .message("Lab orders retrieved successfully")
                                        .data(filtered)
                                        .build());
                } catch (Exception e) {
                        log.error("Failed to list lab orders for patientId {}: {}", patientId, e.getMessage(), e);
                        return ResponseEntity.ok(ApiResponse.<List<LabOrderDto>>builder()
                                        .success(false)
                                        .message("Failed to list lab orders: " + e.getMessage())
                                        .build());
                } finally {
                        RequestContext.clear();
                }
    }

        // ---------- READ one for patient ----------
        @GetMapping("/{patientId}/{id}")
        public ResponseEntity<ApiResponse<LabOrderDto>> getForPatient(
                        @PathVariable Long patientId,
                        @PathVariable Long id) {
                        try {
                                        RequestContext ctx = new RequestContext();
                                        // orgId deprecated; tenantName populated by interceptor.
                                        RequestContext.set(ctx);
                                var dto = service.getOne(id);
                                if (dto == null || dto.getPatientId() == null || !Objects.equals(dto.getPatientId(), patientId)) {
                                        return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                                        .success(false)
                                                        .message("Lab order not found for the specified patient")
                                                        .build());
                                }
                                return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                                .success(true)
                                                .message("Lab order retrieved successfully")
                                                .data(dto)
                                                .build());
                        } catch (IllegalArgumentException e) {
                                log.debug("Lab order not found for id {}: {}", id, e.getMessage());
                                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                                        .body(ApiResponse.<LabOrderDto>builder().success(false).message(e.getMessage()).build());
                        } catch (Exception e) {
                                log.error("Failed to get lab order id {} for patientId {}: {}", id, patientId, e.getMessage(), e);
                                return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                                .success(false)
                                                .message("Failed to retrieve lab order: " + e.getMessage())
                                                .build());
                        } finally {
                                RequestContext.clear();
                        }
    }

    
        // ---------- CREATE for patient ----------
        @PostMapping("/{patientId}")
        public ResponseEntity<ApiResponse<LabOrderDto>> createForPatient(
                        @PathVariable Long patientId,
                        @RequestBody LabOrderDto dto) {
                try {
                                RequestContext ctx = new RequestContext();
                                // orgId deprecated; tenantName populated by interceptor.
                                RequestContext.set(ctx);
                        if (dto.getOrderNumber() == null || !StringUtils.hasText(dto.getOrderNumber())) {
                                return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                                .success(false)
                                                .message("orderNumber is required")
                                                .build());
                        }
                        if (dto.getPatientId() == null) {
                                dto.setPatientId(patientId);
                        } else if (!patientId.equals(dto.getPatientId())) {
                                throw new IllegalArgumentException("patientId in path does not match patientId in payload");
                        }
                        var created = service.create(dto);
                        return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                        .success(true)
                                        .message("Lab order created successfully")
                                        .data(created)
                                        .build());
                } catch (Exception e) {
                        log.error("Failed to create lab order for patientId {}: {}", patientId, e.getMessage(), e);
                        return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                        .success(false)
                                        .message("Failed to create lab order: " + e.getMessage())
                                        .build());
                } finally {
                        RequestContext.clear();
                }
    }

        // ---------- UPDATE for patient ----------
        @PutMapping("/{patientId}/{id}")
        public ResponseEntity<ApiResponse<LabOrderDto>> updateForPatient(
                        @PathVariable Long patientId,
                        @PathVariable Long id,
                        @RequestBody LabOrderDto dto) {
                try {
                                RequestContext ctx = new RequestContext();
                                
                                // orgId deprecated; tenantName populated by interceptor.
                                RequestContext.set(ctx);
                        if (dto.getOrderNumber() == null || !StringUtils.hasText(dto.getOrderNumber())) {
                                return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                                .success(false)
                                                .message("orderNumber is required")
                                                .build());
                        }
                        var existing = service.getOne(id);
                        if (existing == null || existing.getPatientId() == null || !Objects.equals(existing.getPatientId(), patientId)) {
                                return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                                .success(false)
                                                .message("Lab order not found for the specified patient")
                                                .build());
                        }
                        dto.setPatientId(patientId);
                        var updated = service.update(id, dto);
                        return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                        .success(true)
                                        .message("Lab order updated successfully")
                                        .data(updated)
                                        .build());
                } catch (IllegalArgumentException e) {
                        log.debug("Lab order not found for id {}: {}", id, e.getMessage());
                        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                                .body(ApiResponse.<LabOrderDto>builder().success(false).message(e.getMessage()).build());
                } catch (Exception e) {
                        log.error("Failed to update lab order id {} for patientId {}: {}", id, patientId, e.getMessage(), e);
                        return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                                        .success(false)
                                        .message("Failed to update lab order: " + e.getMessage())
                                        .build());
                } finally {
                        
                        RequestContext.clear();
                }
    }

        // ---------- DELETE for patient ----------
        @DeleteMapping("/{patientId}/{id}")
        public ResponseEntity<ApiResponse<Void>> deleteForPatient(
                        @PathVariable Long patientId,
                        @PathVariable Long id) {
                try {
                                RequestContext ctx = new RequestContext();
                                // orgId deprecated; tenantName populated by interceptor.
                                RequestContext.set(ctx);
                        var existing = service.getOne(id);
                        if (existing == null || existing.getPatientId() == null || !Objects.equals(existing.getPatientId(), patientId)) {
                                return ResponseEntity.ok(ApiResponse.<Void>builder()
                                                .success(false)
                                                .message("Lab order not found for the specified patient")
                                                .build());
                        }
                        service.delete(id);
                        return ResponseEntity.ok(ApiResponse.<Void>builder()
                                        .success(true)
                                        .message("Lab order deleted successfully")
                                        .build());
                } catch (IllegalArgumentException e) {
                        log.debug("Lab order not found for id {}: {}", id, e.getMessage());
                        return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                                .body(ApiResponse.<Void>builder().success(false).message(e.getMessage()).build());
                } catch (Exception e) {
                        log.error("Failed to delete lab order id {} for patientId {}: {}", id, patientId, e.getMessage(), e);
                        return ResponseEntity.ok(ApiResponse.<Void>builder()
                                        .success(false)
                                        .message("Failed to delete lab order: " + e.getMessage())
                                        .build());
                } finally {
                        RequestContext.clear();
                }
    }
}
