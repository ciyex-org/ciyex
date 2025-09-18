package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.BillingAutoPayDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.BillingAutoPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/billing")
@Slf4j
public class BillingAutoPayController {

    private final BillingAutoPayService service;

    public BillingAutoPayController(BillingAutoPayService service) {
        this.service = service;
    }

    /* ------------------- SAVE SETTINGS ------------------- */
    @PostMapping("/autopay")
    public ResponseEntity<ApiResponse<BillingAutoPayDto>> saveAutoPay(
            @RequestBody BillingAutoPayDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            BillingAutoPayDto saved = service.save(dto);
            return ResponseEntity.ok(ApiResponse.<BillingAutoPayDto>builder()
                    .success(true)
                    .message("AutoPay settings saved successfully")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            log.error("Failed to save AutoPay: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<BillingAutoPayDto>builder()
                    .success(false)
                    .message("Failed to save AutoPay: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET SETTINGS ------------------- */
    @GetMapping("/autopay/{userId}")
    public ResponseEntity<ApiResponse<BillingAutoPayDto>> getAutoPay(
            @PathVariable Long userId,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            Optional<BillingAutoPayDto> autoPay = service.getByUser(userId);
            return autoPay.map(c -> ResponseEntity.ok(ApiResponse.<BillingAutoPayDto>builder()
                            .success(true)
                            .message("AutoPay retrieved successfully")
                            .data(c)
                            .build()))
                    .orElseGet(() -> ResponseEntity.ok(ApiResponse.<BillingAutoPayDto>builder()
                            .success(false)
                            .message("No AutoPay settings found for user " + userId)
                            .build()));
        } catch (Exception e) {
            log.error("Failed to retrieve AutoPay for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<BillingAutoPayDto>builder()
                    .success(false)
                    .message("Failed to retrieve AutoPay: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- UPDATE SETTINGS ------------------- */
    @PutMapping("/autopay/{id}")
    public ResponseEntity<ApiResponse<BillingAutoPayDto>> updateAutoPay(
            @PathVariable Long id,
            @RequestBody BillingAutoPayDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            BillingAutoPayDto updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<BillingAutoPayDto>builder()
                    .success(true)
                    .message("AutoPay updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update AutoPay id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<BillingAutoPayDto>builder()
                    .success(false)
                    .message("Failed to update AutoPay: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- DELETE SETTINGS ------------------- */
    @DeleteMapping("/autopay/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAutoPay(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("AutoPay deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete AutoPay id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete AutoPay: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }
}
