package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.BillingHistoryService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    @Value("${stripe.webhook-secret:}")
    private String endpointSecret;

    private final BillingHistoryService billingHistoryService;

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

        // Handle success
        if ("payment_intent.succeeded".equals(event.getType())) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            deserializer.getObject().ifPresent(obj -> {
                PaymentIntent succeededIntent = (PaymentIntent) obj;
                billingHistoryService.updateStatus(succeededIntent.getId(), succeededIntent.getStatus());
                log.info("💰 Payment succeeded: {}", succeededIntent.getId());
            });
        }
        // Handle failure
        else if ("payment_intent.payment_failed".equals(event.getType())) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            deserializer.getObject().ifPresent(obj -> {
                PaymentIntent failedIntent = (PaymentIntent) obj;
                billingHistoryService.updateStatus(failedIntent.getId(), failedIntent.getStatus());
                log.warn("❌ Payment failed: {}", failedIntent.getId());
            });
        }
        // Handle other intents like processing or canceled
        else if ("payment_intent.processing".equals(event.getType())) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            deserializer.getObject().ifPresent(obj -> {
                PaymentIntent processingIntent = (PaymentIntent) obj;
                billingHistoryService.updateStatus(processingIntent.getId(), processingIntent.getStatus());
                log.info("⏳ Payment processing: {}", processingIntent.getId());
            });
        }
        else {
            log.info("⚠️ Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("✅ Webhook received");
    }
}
