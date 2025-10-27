package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.StripeBillingCardDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.PaymentOrderService;

import com.qiaben.ciyex.service.StripeBillingCardService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.qiaben.ciyex.service.BillingHistoryService;   // updated

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    @Value("${stripe.webhook-secret:}")
    private String endpointSecret;

    private final BillingHistoryService billingHistoryService;  // updated
    private final StripeBillingCardService billingCardService;
    private final PaymentOrderService paymentOrderService;

    private Long getOrgIdOrThrow() {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) {
            throw new RuntimeException("Organization context is required");
        }
        return ctx.getOrgId();
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            HttpServletRequest request,
            @RequestBody String payload
    ) {
        String sigHeader = request.getHeader("Stripe-Signature");
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("⚠️ Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.badRequest().body("⚠️ Invalid signature");
        }

        log.info("🔔 Received Stripe event: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                PaymentIntent succeededIntent = getObject(event, PaymentIntent.class);
                if (succeededIntent != null) {
                    billingHistoryService.updateStatus(
                            succeededIntent.getId(),
                            succeededIntent.getStatus()
                    );
                    paymentOrderService.markOrderAsPaid(succeededIntent.getId());
                    log.info("💰 Payment succeeded: {}", succeededIntent.getId());
                }
            }
            case "payment_intent.payment_failed" -> {
                PaymentIntent failedIntent = getObject(event, PaymentIntent.class);
                if (failedIntent != null) {
                    billingHistoryService.updateStatus(
                            failedIntent.getId(),
                            failedIntent.getStatus()
                    );
                    paymentOrderService.markOrderAsFailed(failedIntent.getId());
                    log.warn("❌ Payment failed: {}", failedIntent.getId());
                }
            }
            case "payment_intent.processing" -> {
                PaymentIntent processingIntent = getObject(event, PaymentIntent.class);
                if (processingIntent != null) {
                    billingHistoryService.updateStatus(
                            processingIntent.getId(),
                            processingIntent.getStatus()
                    );
                    log.info("⏳ Payment processing: {}", processingIntent.getId());
                }
            }
            case "payment_method.attached" -> {
                PaymentMethod pm = getObject(event, PaymentMethod.class);
                if (pm != null && pm.getCard() != null) {
                    StripeBillingCardDto dto = StripeBillingCardDto.builder()
                            .stripePaymentMethodId(pm.getId())
                            .stripeCustomerId(pm.getCustomer() != null ? pm.getCustomer().toString() : null)
                            .brand(pm.getCard().getBrand())
                            .last4(pm.getCard().getLast4())
                            .expMonth(pm.getCard().getExpMonth() != null ? pm.getCard().getExpMonth().intValue() : null)
                            .expYear(pm.getCard().getExpYear() != null ? pm.getCard().getExpYear().intValue() : null)
                            .defaultCard(false)
                            .orgId(getOrgIdOrThrow())
                            .build();

                    billingCardService.create(dto, getOrgIdOrThrow());
                    log.info("💳 Stripe payment method attached & saved: {}", pm.getId());
                }
            }
            default -> log.info("⚠️ Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("✅ Webhook received");
    }

    private <T> T getObject(Event event, Class<T> clazz) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Object obj = deserializer.getObject().get();
            if (clazz.isInstance(obj)) {
                return clazz.cast(obj);
            }
        }
        return null;
    }
}
