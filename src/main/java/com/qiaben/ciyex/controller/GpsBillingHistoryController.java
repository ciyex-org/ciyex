package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.GpsBillingHistoryDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.GpsBillingHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/gps/billing")
@Slf4j
public class GpsBillingHistoryController {

    private final GpsBillingHistoryService service;

    public GpsBillingHistoryController(GpsBillingHistoryService service) {
        this.service = service;
    }

    /* ------------------- PAYMENT ------------------- */
    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<GpsBillingHistoryDto>> processPayment(
            @Valid @RequestBody GpsBillingHistoryDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            dto.setOrgId(orgId); // enforce tenant
            GpsBillingHistoryDto result = service.recordPayment(dto);

            return ResponseEntity.ok(ApiResponse.<GpsBillingHistoryDto>builder()
                    .success(true)
                    .message("GPS payment processed successfully")
                    .data(result)
                    .build());
        } catch (Exception e) {
            log.error("Failed to process GPS payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<GpsBillingHistoryDto>builder()
                            .success(false)
                            .message("Failed to process GPS payment: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- READ ------------------- */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<GpsBillingHistoryDto>>> getAllHistory(
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            List<GpsBillingHistoryDto> history = service.getAll();
            return ResponseEntity.ok(ApiResponse.<List<GpsBillingHistoryDto>>builder()
                    .success(true)
                    .message("GPS billing history retrieved successfully")
                    .data(history)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve GPS billing history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<GpsBillingHistoryDto>>builder()
                            .success(false)
                            .message("Failed to retrieve GPS billing history: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping("/history/user/{userId}")
    public ResponseEntity<ApiResponse<List<GpsBillingHistoryDto>>> getHistoryByUser(
            @PathVariable Long userId,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            List<GpsBillingHistoryDto> history = service.getAllByUser(userId);
            return ResponseEntity.ok(ApiResponse.<List<GpsBillingHistoryDto>>builder()
                    .success(true)
                    .message("GPS billing history retrieved successfully")
                    .data(history)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve GPS billing history for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<GpsBillingHistoryDto>>builder()
                            .success(false)
                            .message("Failed to retrieve GPS billing history: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }
}