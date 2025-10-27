package com.qiaben.ciyex.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.qiaben.ciyex.service.StripeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StripeController {

    private final StripeService stripeService;
    private final com.qiaben.ciyex.service.PaymentOrderService paymentOrderService;

    @Value("${stripe.publishable-key:}")
    private String publishableKey;

    @GetMapping("/api/stripe/config")
    public Map<String, String> getStripeConfig() {
        return Map.of("publishableKey", publishableKey);
    }

    @PostMapping("/api/payments/stripe/{orgId}")
    public ResponseEntity<?> createPayment(
            @PathVariable Long orgId,
            @RequestParam long amountCents,
            @RequestParam String currency,
            @RequestParam(required = false) String description
    ) throws StripeException {
        PaymentIntent intent = stripeService.createPaymentIntent(orgId, amountCents, currency, description);
        return ResponseEntity.ok(Map.of("clientSecret", intent.getClientSecret()));
    }

    @PostMapping("/api/payments/stripe/webhook/{orgId}")
    public ResponseEntity<String> stripeWebhook(
            @PathVariable Long orgId,
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader
    ) {
        try {
            Event event = stripeService.verifyWebhook(orgId, payload, sigHeader);
            log.info("Received Stripe event={} for org={}", event.getType(), orgId);

            // handle a couple of events
            switch (event.getType()) {
                case "payment_intent.succeeded": {
                    // deserialize PaymentIntent
                    var des = event.getDataObjectDeserializer();
                    PaymentIntent pi = (PaymentIntent) des.getObject().orElse(null);
                    if (pi != null) {
                        paymentOrderService.markOrderAsPaid(pi.getId());
                    }
                    break;
                }
                case "payment_intent.payment_failed": {
                    var des = event.getDataObjectDeserializer();
                    PaymentIntent pi = (PaymentIntent) des.getObject().orElse(null);
                    if (pi != null) {
                        paymentOrderService.markOrderAsFailed(pi.getId());
                    }
                    break;
                }
                default:
                    break;
            }

            return ResponseEntity.ok("success");
        } catch (Exception e) {
            log.error("Stripe webhook verification failed for org={}", orgId, e);
            return ResponseEntity.status(400).body("invalid signature");
        }
    }
}
