package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.BillingHistoryDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.BillingHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing-history")
@RequiredArgsConstructor
@Slf4j
public class BillingHistoryController {

    private final BillingHistoryService service;

    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<BillingHistoryDto>> pay(
            @RequestBody BillingHistoryDto dto,
            @RequestHeader("X-Org-Id") Long orgId) {
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);

        try {
            BillingHistoryDto saved = service.recordPayment(dto);
            return ResponseEntity.ok(ApiResponse.<BillingHistoryDto>builder()
                    .success(true)
                    .message("Payment processed successfully")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            log.error("Payment failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<BillingHistoryDto>builder()
                            .success(false)
                            .message("Payment failed: " + e.getMessage())
                            .build());
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BillingHistoryDto>>> getAll(
            @RequestHeader("X-Org-Id") Long orgId) {
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);

        try {
            List<BillingHistoryDto> history = service.getAll();
            return ResponseEntity.ok(ApiResponse.<List<BillingHistoryDto>>builder()
                    .success(true)
                    .message("Retrieved successfully")
                    .data(history)
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BillingHistoryDto>>> getByUser(
            @PathVariable Long userId,
            @RequestHeader("X-Org-Id") Long orgId) {
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);

        try {
            List<BillingHistoryDto> history = service.getByUser(userId);
            return ResponseEntity.ok(ApiResponse.<List<BillingHistoryDto>>builder()
                    .success(true)
                    .message("Retrieved successfully")
                    .data(history)
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<BillingHistoryDto>> archive(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);

        try {
            BillingHistoryDto updated = service.archive(id);
            return ResponseEntity.ok(ApiResponse.<BillingHistoryDto>builder()
                    .success(true)
                    .message("Archived successfully")
                    .data(updated)
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @PutMapping("/{id}/unarchive")
    public ResponseEntity<ApiResponse<BillingHistoryDto>> unarchive(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);

        try {
            BillingHistoryDto updated = service.unarchive(id);
            return ResponseEntity.ok(ApiResponse.<BillingHistoryDto>builder()
                    .success(true)
                    .message("Unarchived successfully")
                    .data(updated)
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);

        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Deleted successfully")
                    .build());
        } finally {
            RequestContext.clear();
        }
    }
}
