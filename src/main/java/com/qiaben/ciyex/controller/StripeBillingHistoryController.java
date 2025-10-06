package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.StripeBillingHistoryDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.StripeBillingHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stripe/billing-history")
@RequiredArgsConstructor
@Slf4j
public class StripeBillingHistoryController {

    private final StripeBillingHistoryService service;

    private void setOrgContext(Long orgId) {
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);
    }

    /* ------------------- PAY NOW ------------------- */
    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<StripeBillingHistoryDto>> pay(
            @RequestBody StripeBillingHistoryDto dto,
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            StripeBillingHistoryDto saved = service.recordPayment(dto);
            return ResponseEntity.ok(ApiResponse.<StripeBillingHistoryDto>builder()
                    .success(true)
                    .message("Payment processed successfully")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            log.error("❌ Payment failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<StripeBillingHistoryDto>builder()
                            .success(false)
                            .message("Payment failed: " + e.getMessage())
                            .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET ALL ------------------- */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StripeBillingHistoryDto>>> getAll(
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            List<StripeBillingHistoryDto> history = service.getAll();
            return ResponseEntity.ok(ApiResponse.<List<StripeBillingHistoryDto>>builder()
                    .success(true)
                    .message("Retrieved successfully")
                    .data(history)
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET BY USER ------------------- */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<StripeBillingHistoryDto>>> getByUser(
            @PathVariable Long userId,
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            List<StripeBillingHistoryDto> history = service.getByUser(userId);
            return ResponseEntity.ok(ApiResponse.<List<StripeBillingHistoryDto>>builder()
                    .success(true)
                    .message("Retrieved successfully")
                    .data(history)
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- ARCHIVE ------------------- */
    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<StripeBillingHistoryDto>> archive(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            StripeBillingHistoryDto updated = service.archive(id);
            return ResponseEntity.ok(ApiResponse.<StripeBillingHistoryDto>builder()
                    .success(true)
                    .message("Archived successfully")
                    .data(updated)
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- UNARCHIVE ------------------- */
    @PutMapping("/{id}/unarchive")
    public ResponseEntity<ApiResponse<StripeBillingHistoryDto>> unarchive(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            StripeBillingHistoryDto updated = service.unarchive(id);
            return ResponseEntity.ok(ApiResponse.<StripeBillingHistoryDto>builder()
                    .success(true)
                    .message("Unarchived successfully")
                    .data(updated)
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- DELETE ------------------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
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
