package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.GpsBillingCardDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.GpsBillingCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/gps/cards")
@RequiredArgsConstructor
public class GpsBillingCardController {

    private final GpsBillingCardService service;

    /* CREATE */
    @PostMapping
    public ResponseEntity<ApiResponse<GpsBillingCardDto>> createCard(
            @Valid @RequestBody GpsBillingCardDto dto) {
        try {
            GpsBillingCardDto saved = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(true)
                    .message("Card saved successfully")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create card", e);
            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(false)
                    .message("Failed to save card: " + e.getMessage())
                    .build());
        }
    }

    /* ALIAS for /api/gps/billing/tokenize */
    @PostMapping("/billing/tokenize")
    public ResponseEntity<ApiResponse<GpsBillingCardDto>> tokenize(
            @Valid @RequestBody GpsBillingCardDto dto) {
        return createCard(dto);
    }

    /* GET ALL */
    @GetMapping
    public ResponseEntity<ApiResponse<List<GpsBillingCardDto>>> getAll() {
        try {
            List<GpsBillingCardDto> cards = service.getAll();
            return ResponseEntity.ok(ApiResponse.<List<GpsBillingCardDto>>builder()
                    .success(true)
                    .message("Cards fetched successfully")
                    .data(cards)
                    .build());
        } catch (Exception e) {
            log.error("Failed to fetch cards", e);
            return ResponseEntity.ok(ApiResponse.<List<GpsBillingCardDto>>builder()
                    .success(false)
                    .message("Failed to fetch cards: " + e.getMessage())
                    .build());
        }
    }

    /* GET BY USER */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<GpsBillingCardDto>>> getByUser(
            @PathVariable UUID userId) {
        try {
            List<GpsBillingCardDto> cards = service.getAllByUser(userId);
            return ResponseEntity.ok(ApiResponse.<List<GpsBillingCardDto>>builder()
                    .success(true)
                    .message("User cards fetched successfully")
                    .data(cards)
                    .build());
        } catch (Exception e) {
            log.error("Failed to fetch user cards", e);
            return ResponseEntity.ok(ApiResponse.<List<GpsBillingCardDto>>builder()
                    .success(false)
                    .message("Failed to fetch user cards: " + e.getMessage())
                    .build());
        }
    }

    /* GET BY ID */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GpsBillingCardDto>> getById(@PathVariable Long id) {
        try {
            return service.getById(id)
                    .map(dto -> ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                            .success(true)
                            .message("Card found")
                            .data(dto)
                            .build()))
                    .orElse(ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                            .success(false)
                            .message("Card not found with id: " + id)
                            .build()));
        } catch (Exception e) {
            log.error("Failed to get card", e);
            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(false)
                    .message("Failed to get card: " + e.getMessage())
                    .build());
        }
    }

    /* UPDATE */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GpsBillingCardDto>> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody GpsBillingCardDto dto) {
        try {
            GpsBillingCardDto updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(true)
                    .message("Card updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update card", e);
            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(false)
                    .message("Failed to update card: " + e.getMessage())
                    .build());
        }
    }

    /* DELETE */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Card deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete card", e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete card: " + e.getMessage())
                    .build());
        }
    }

    /* SET DEFAULT */
    @PostMapping("/{id}/default")
    public ResponseEntity<ApiResponse<GpsBillingCardDto>> setDefaultCard(@PathVariable Long id) {
        try {
            GpsBillingCardDto updated = service.setDefault(id);
            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(true)
                    .message("Default card updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to set default card", e);
            return ResponseEntity.ok(ApiResponse.<GpsBillingCardDto>builder()
                    .success(false)
                    .message("Failed to set default card: " + e.getMessage())
                    .build());
        }
    }
}
