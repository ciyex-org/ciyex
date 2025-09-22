package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.BillingHistoryDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.BillingHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing/history")
@Slf4j
public class BillingHistoryController {

    private final BillingHistoryService service;

    public BillingHistoryController(BillingHistoryService service) {
        this.service = service;
    }

    /* ------------------- PAY NOW ------------------- */
    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<BillingHistoryDto>> pay(
            @RequestBody BillingHistoryDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            if (dto.getUserId() == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.<BillingHistoryDto>builder()
                                .success(false)
                                .message("userId is required to process payment")
                                .build()
                );
            }

            BillingHistoryDto saved = service.recordPayment(dto);
            return ResponseEntity.ok(ApiResponse.<BillingHistoryDto>builder()
                    .success(true)
                    .message("Payment processed successfully")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            log.error("Payment failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<BillingHistoryDto>builder()
                            .success(false)
                            .message("Payment failed: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET BY USER ------------------- */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BillingHistoryDto>>> getByUser(
            @PathVariable Long userId,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            List<BillingHistoryDto> history = service.getByUser(userId);
            return ResponseEntity.ok(ApiResponse.<List<BillingHistoryDto>>builder()
                    .success(true)
                    .message("Retrieved successfully")
                    .data(history)
                    .build());
        } catch (Exception e) {
            log.error("Failed to fetch billing history by user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<BillingHistoryDto>>builder()
                            .success(false)
                            .message("Failed to retrieve billing history: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET ALL ------------------- */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BillingHistoryDto>>> getAll(
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            List<BillingHistoryDto> history = service.getAll();
            return ResponseEntity.ok(ApiResponse.<List<BillingHistoryDto>>builder()
                    .success(true)
                    .message("Retrieved successfully")
                    .data(history)
                    .build());
        } catch (Exception e) {
            log.error("Failed to fetch billing history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<BillingHistoryDto>>builder()
                            .success(false)
                            .message("Failed to retrieve billing history: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- DELETE ------------------- */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id,
                                                    @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete billing history {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete billing history: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- ARCHIVE ------------------- */
    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<BillingHistoryDto>> archive(@PathVariable Long id,
                                                                  @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            BillingHistoryDto updated = service.archive(id);
            return ResponseEntity.ok(ApiResponse.<BillingHistoryDto>builder()
                    .success(true)
                    .message("Archived successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to archive billing history {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<BillingHistoryDto>builder()
                            .success(false)
                            .message("Failed to archive billing history: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- UNARCHIVE ------------------- */
    @PutMapping("/{id}/unarchive")
    public ResponseEntity<ApiResponse<BillingHistoryDto>> unarchive(@PathVariable Long id,
                                                                    @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            BillingHistoryDto updated = service.unarchive(id);
            return ResponseEntity.ok(ApiResponse.<BillingHistoryDto>builder()
                    .success(true)
                    .message("Unarchived successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to unarchive billing history {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<BillingHistoryDto>builder()
                            .success(false)
                            .message("Failed to unarchive billing history: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }
}
