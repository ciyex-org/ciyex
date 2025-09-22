package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.BillingCardDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.BillingCardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
@Slf4j
public class BillingCardController {

    private final BillingCardService service;

    public BillingCardController(BillingCardService service) {
        this.service = service;
    }

    /* ------------------- CREATE / TOKENIZE ------------------- */
    @PostMapping("/tokenize")
    public ResponseEntity<ApiResponse<BillingCardDto>> saveCard(
            @Valid @RequestBody BillingCardDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            dto.setOrgId(orgId); // enforce tenant
            BillingCardDto saved = service.create(dto);

            return ResponseEntity.ok(ApiResponse.<BillingCardDto>builder()
                    .success(true)
                    .message("Card saved successfully")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            log.error("Failed to save card: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<BillingCardDto>builder()
                            .success(false)
                            .message("Failed to save card: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- GET CARDS FOR USER ------------------- */
    @GetMapping("/cards/{userId}")
    public ResponseEntity<ApiResponse<List<BillingCardDto>>> getCardsForUser(
            @PathVariable Long userId,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            List<BillingCardDto> cards = service.getAllByUser(userId, orgId);
            return ResponseEntity.ok(ApiResponse.<List<BillingCardDto>>builder()
                    .success(true)
                    .message("Cards retrieved successfully")
                    .data(cards)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve cards for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<BillingCardDto>>builder()
                            .success(false)
                            .message("Failed to retrieve cards: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- DELETE ------------------- */
    @DeleteMapping("/card/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            service.delete(id, orgId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Card deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete card id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete card: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- SET DEFAULT ------------------- */
    @PutMapping("/card/{id}/default")
    public ResponseEntity<ApiResponse<BillingCardDto>> setDefaultCard(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            BillingCardDto updated = service.setDefault(id, orgId);
            return ResponseEntity.ok(ApiResponse.<BillingCardDto>builder()
                    .success(true)
                    .message("Default card updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to set default card id {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<BillingCardDto>builder()
                            .success(false)
                            .message("Failed to set default card: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }
}
