package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.GpsBillingCardDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.GpsBillingCardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/gps/billing")
@Slf4j
public class GpsBillingCardController {

    private final GpsBillingCardService service;

    public GpsBillingCardController(GpsBillingCardService service) {
        this.service = service;
    }

    /* ------------------- CREATE / TOKENIZE ------------------- */
    @PostMapping("/tokenize")
    public ResponseEntity<ApiResponse<GpsBillingCardDto>> saveCard(
            @Valid @RequestBody GpsBillingCardDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            dto.setOrgId(orgId); // enforce tenant
            GpsBillingCardDto saved = service.create(dto);

            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(true)
                    .message("GPS card saved successfully")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            log.error("Failed to save GPS card: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<GpsBillingCardDto>builder()
                            .success(false)
                            .message("Failed to save GPS card: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- READ ------------------- */
    @GetMapping("/cards")
    public ResponseEntity<ApiResponse<List<GpsBillingCardDto>>> getAllCards(
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            List<GpsBillingCardDto> cards = service.getAll();
            return ResponseEntity.ok(ApiResponse.<List<GpsBillingCardDto>>builder()
                    .success(true)
                    .message("GPS cards retrieved successfully")
                    .data(cards)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve GPS cards: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<GpsBillingCardDto>>builder()
                            .success(false)
                            .message("Failed to retrieve GPS cards: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping("/cards/user/{userId}")
    public ResponseEntity<ApiResponse<List<GpsBillingCardDto>>> getCardsByUser(
            @PathVariable Long userId,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            List<GpsBillingCardDto> cards = service.getAllByUser(userId);
            return ResponseEntity.ok(ApiResponse.<List<GpsBillingCardDto>>builder()
                    .success(true)
                    .message("GPS cards retrieved successfully")
                    .data(cards)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve GPS cards for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<List<GpsBillingCardDto>>builder()
                            .success(false)
                            .message("Failed to retrieve GPS cards: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<ApiResponse<GpsBillingCardDto>> getCard(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            return service.getById(id)
                    .map(card -> ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                            .success(true)
                            .message("GPS card retrieved successfully")
                            .data(card)
                            .build()))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to retrieve GPS card {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<GpsBillingCardDto>builder()
                            .success(false)
                            .message("Failed to retrieve GPS card: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- UPDATE ------------------- */
    @PutMapping("/cards/{id}")
    public ResponseEntity<ApiResponse<GpsBillingCardDto>> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody GpsBillingCardDto dto,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            GpsBillingCardDto updated = service.update(id, dto, orgId);
            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(true)
                    .message("GPS card updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update GPS card {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<GpsBillingCardDto>builder()
                            .success(false)
                            .message("Failed to update GPS card: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- DELETE ------------------- */
    @DeleteMapping("/cards/{id}")
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
                    .message("GPS card deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete GPS card {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete GPS card: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }

    /* ------------------- SET DEFAULT ------------------- */
    @PostMapping("/cards/{id}/set-default")
    public ResponseEntity<ApiResponse<GpsBillingCardDto>> setDefaultCard(
            @PathVariable Long id,
            @RequestHeader("x-org-id") Long orgId) {
        try {
            RequestContext ctx = new RequestContext();
            ctx.setOrgId(orgId);
            RequestContext.set(ctx);

            GpsBillingCardDto updated = service.setDefault(id, orgId);
            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(true)
                    .message("GPS default card set successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to set GPS default card {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.<GpsBillingCardDto>builder()
                            .success(false)
                            .message("Failed to set GPS default card: " + e.getMessage())
                            .build()
            );
        } finally {
            RequestContext.clear();
        }
    }
}