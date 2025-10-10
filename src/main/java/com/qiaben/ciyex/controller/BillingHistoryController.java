package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.BillingHistoryDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.BillingHistory.BillingProvider;
import com.qiaben.ciyex.service.BillingHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingHistoryController {

    private final BillingHistoryService billingService;

    private void setOrgContext(Long orgId) {
        RequestContext ctx = new RequestContext();
        ctx.setOrgId(orgId);
        RequestContext.set(ctx);
    }

    private <T> ResponseEntity<ApiResponse<T>> success(String message, T data) {
        return ResponseEntity.ok(ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build());
    }

    private <T> ResponseEntity<ApiResponse<T>> failure(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<T>builder()
                        .success(false)
                        .message(message)
                        .build());
    }

    /* ------------------- STRIPE PAY ------------------- */
    @PostMapping("/pay/stripe")
    public ResponseEntity<ApiResponse<BillingHistoryDto>> payStripe(
            @Valid @RequestBody BillingHistoryDto dto,
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            dto.setOrgId(orgId);
            dto.setProvider(BillingProvider.STRIPE);
            BillingHistoryDto saved = billingService.recordStripePayment(dto);
            return success("Stripe payment processed successfully", saved);
        } catch (Exception e) {
            log.error("Stripe payment failed: {}", e.getMessage(), e);
            return failure("Stripe payment failed: " + e.getMessage());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GPS PAY ------------------- */
    @PostMapping("/pay/gps")
    public ResponseEntity<ApiResponse<BillingHistoryDto>> payGps(
            @Valid @RequestBody BillingHistoryDto dto,
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            dto.setOrgId(orgId);
            dto.setProvider(BillingProvider.GPS);
            BillingHistoryDto saved = billingService.recordGpsPayment(dto);
            return success("GPS payment processed successfully", saved);
        } catch (Exception e) {
            log.error("GPS payment failed: {}", e.getMessage(), e);
            return failure("GPS payment failed: " + e.getMessage());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET ALL ------------------- */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<BillingHistoryDto>>> getAllHistory(
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            List<BillingHistoryDto> history = billingService.getAll();
            return success("Billing history retrieved successfully", history);
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET BY USER ------------------- */
    @GetMapping("/history/user/{userId}")
    public ResponseEntity<ApiResponse<List<BillingHistoryDto>>> getHistoryByUser(
            @PathVariable Long userId,
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            List<BillingHistoryDto> history = billingService.getByUser(userId);
            return success("Billing history retrieved successfully", history);
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- DELETE ------------------- */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHistory(
            @PathVariable Long id,
            @RequestHeader("X-Org-Id") Long orgId) {

        setOrgContext(orgId);
        try {
            billingService.delete(id);
            return success("Billing history deleted successfully", null);
        } finally {
            RequestContext.clear();
        }
    }
}
