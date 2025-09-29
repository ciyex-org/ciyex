package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.GpsPaymentDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.GpsPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/gps/payments")
@Slf4j
public class GpsPaymentController {

    private final GpsPaymentService service;

    public GpsPaymentController(GpsPaymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GpsPaymentDto>> create(
            @Valid @RequestBody GpsPaymentDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            GpsPaymentDto saved = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<GpsPaymentDto>builder()
                    .success(true)
                    .message("GPS payment created successfully")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create GPS payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<GpsPaymentDto>builder()
                            .success(false)
                            .message("Failed to create GPS payment: " + e.getMessage())
                            .build());
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GpsPaymentDto>>> getAll(@RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            List<GpsPaymentDto> payments = service.getAll();
            return ResponseEntity.ok(ApiResponse.<List<GpsPaymentDto>>builder()
                    .success(true)
                    .message("GPS payments retrieved successfully")
                    .data(payments)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve GPS payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<GpsPaymentDto>>builder()
                            .success(false)
                            .message("Failed to retrieve GPS payments: " + e.getMessage())
                            .build());
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GpsPaymentDto>> getById(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            return service.getById(id)
                    .map(payment -> ResponseEntity.ok(ApiResponse.<GpsPaymentDto>builder()
                            .success(true)
                            .message("GPS payment retrieved successfully")
                            .data(payment)
                            .build()))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to retrieve GPS payment {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<GpsPaymentDto>builder()
                            .success(false)
                            .message("Failed to retrieve GPS payment: " + e.getMessage())
                            .build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GpsPaymentDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody GpsPaymentDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            GpsPaymentDto updated = service.update(id, dto, orgId);
            return ResponseEntity.ok(ApiResponse.<GpsPaymentDto>builder()
                    .success(true)
                    .message("GPS payment updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update GPS payment {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<GpsPaymentDto>builder()
                            .success(false)
                            .message("Failed to update GPS payment: " + e.getMessage())
                            .build());
        } finally {
            RequestContext.clear();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            service.delete(id, orgId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("GPS payment deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete GPS payment {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete GPS payment: " + e.getMessage())
                            .build());
        } finally {
            RequestContext.clear();
        }
    }
}
