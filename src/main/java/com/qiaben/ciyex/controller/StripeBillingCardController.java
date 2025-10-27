package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.StripeBillingCardDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.OrgConfigService;
import com.qiaben.ciyex.service.StripeBillingCardService;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.net.RequestOptions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
// Support both /api/stripe/cards and /api/stripe-billing-cards
@RequestMapping({"/api/stripe/cards", "/api/stripe-billing-cards"})
@RequiredArgsConstructor
public class StripeBillingCardController {

    private final StripeBillingCardService service;
    private final OrgConfigService orgConfigService;

    private Long getOrgIdOrThrow() {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) {
            throw new RuntimeException("Organization context is required");
        }
        return ctx.getOrgId();
    }

    /* CREATE */
    @PostMapping
    public ResponseEntity<ApiResponse<StripeBillingCardDto>> createCard(
            @Valid @RequestBody StripeBillingCardDto dto) {
        Long orgId = getOrgIdOrThrow();

        try {
            // 1. Get org-specific Stripe key
            String stripeKey = orgConfigService.getStripeSecretKey(orgId);
            RequestOptions opts = RequestOptions.builder().setApiKey(stripeKey).build();

            // 2. Ensure Stripe Customer exists
            String customerId = dto.getStripeCustomerId();
            if (customerId == null || customerId.isBlank()) {
                Map<String, Object> custParams = new HashMap<>();
                custParams.put("description", "Org " + orgId + " user " + dto.getUserId());
                custParams.put("metadata", Map.of("orgId", String.valueOf(orgId)));
                Customer customer = Customer.create(custParams, opts);
                customerId = customer.getId();
                dto.setStripeCustomerId(customerId);
            }

            // 3. Attach PaymentMethod to Customer (if provided)
            if (dto.getStripePaymentMethodId() != null && !dto.getStripePaymentMethodId().isBlank()) {
                PaymentMethod pm = PaymentMethod.retrieve(dto.getStripePaymentMethodId(), opts);

                // ⚠️ Avoid re-attaching if already attached
                if (pm.getCustomer() == null || !pm.getCustomer().equals(customerId)) {
                    pm.attach(Map.of("customer", customerId), opts);
                }
            }

            // 4. Save in DB
            StripeBillingCardDto saved = service.create(dto, orgId);

            return ResponseEntity.ok(ApiResponse.<StripeBillingCardDto>builder()
                    .success(true)
                    .message("Stripe card saved successfully")
                    .data(saved)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create stripe card", e);
            return ResponseEntity.ok(ApiResponse.<StripeBillingCardDto>builder()
                    .success(false)
                    .message("Failed to save card: " + e.getMessage())
                    .build());
        }
    }

    /* ALIAS for /api/stripe/cards/billing/tokenize */
    @PostMapping("/billing/tokenize")
    public ResponseEntity<ApiResponse<StripeBillingCardDto>> tokenize(
            @Valid @RequestBody StripeBillingCardDto dto) {
        return createCard(dto);
    }

    /* GET ALL */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StripeBillingCardDto>>> getAll() {
        try {
            List<StripeBillingCardDto> cards = service.getAll(getOrgIdOrThrow());
            return ResponseEntity.ok(ApiResponse.<List<StripeBillingCardDto>>builder()
                    .success(true)
                    .message("Stripe cards fetched successfully")
                    .data(cards)
                    .build());
        } catch (Exception e) {
            log.error("Failed to fetch stripe cards", e);
            return ResponseEntity.ok(ApiResponse.<List<StripeBillingCardDto>>builder()
                    .success(false)
                    .message("Failed to fetch cards: " + e.getMessage())
                    .build());
        }
    }

    /* GET BY USER */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<StripeBillingCardDto>>> getByUser(
            @PathVariable Long userId) {
        try {
            List<StripeBillingCardDto> cards = service.getAllByUser(userId, getOrgIdOrThrow());
            return ResponseEntity.ok(ApiResponse.<List<StripeBillingCardDto>>builder()
                    .success(true)
                    .message("User stripe cards fetched successfully")
                    .data(cards)
                    .build());
        } catch (Exception e) {
            log.error("Failed to fetch user stripe cards", e);
            return ResponseEntity.ok(ApiResponse.<List<StripeBillingCardDto>>builder()
                    .success(false)
                    .message("Failed to fetch user stripe cards: " + e.getMessage())
                    .build());
        }
    }

    /* GET BY ID */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StripeBillingCardDto>> getById(@PathVariable Long id) {
        try {
            return service.getById(id, getOrgIdOrThrow())
                    .map(dto -> ResponseEntity.ok(ApiResponse.<StripeBillingCardDto>builder()
                            .success(true)
                            .message("Stripe card found")
                            .data(dto)
                            .build()))
                    .orElse(ResponseEntity.ok(ApiResponse.<StripeBillingCardDto>builder()
                            .success(false)
                            .message("Card not found with id: " + id)
                            .build()));
        } catch (Exception e) {
            log.error("Failed to get stripe card", e);
            return ResponseEntity.ok(ApiResponse.<StripeBillingCardDto>builder()
                    .success(false)
                    .message("Failed to get card: " + e.getMessage())
                    .build());
        }
    }

    /* UPDATE */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StripeBillingCardDto>> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody StripeBillingCardDto dto) {
        try {
            StripeBillingCardDto updated = service.update(id, dto, getOrgIdOrThrow());
            return ResponseEntity.ok(ApiResponse.<StripeBillingCardDto>builder()
                    .success(true)
                    .message("Stripe card updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update stripe card", e);
            return ResponseEntity.ok(ApiResponse.<StripeBillingCardDto>builder()
                    .success(false)
                    .message("Failed to update card: " + e.getMessage())
                    .build());
        }
    }

    /* DELETE */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCard(@PathVariable Long id) {
        try {
            service.delete(id, getOrgIdOrThrow());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Stripe card deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete stripe card", e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete card: " + e.getMessage())
                    .build());
        }
    }

    /* SET DEFAULT */
    @PostMapping("/{id}/default")
    public ResponseEntity<ApiResponse<StripeBillingCardDto>> setDefaultCard(@PathVariable Long id) {
        try {
            StripeBillingCardDto updated = service.setDefault(id, getOrgIdOrThrow());
            return ResponseEntity.ok(ApiResponse.<StripeBillingCardDto>builder()
                    .success(true)
                    .message("Default stripe card updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to set default stripe card", e);
            return ResponseEntity.ok(ApiResponse.<StripeBillingCardDto>builder()
                    .success(false)
                    .message("Failed to set default card: " + e.getMessage())
                    .build());
        }
    }
}
