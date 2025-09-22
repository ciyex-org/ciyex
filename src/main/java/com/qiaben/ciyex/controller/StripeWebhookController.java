package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.BillingHistoryService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    @Value("${stripe.webhook-secret:}")
    private String endpointSecret;

    private final BillingHistoryService billingHistoryService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request, @RequestBody String payload) {
        String sigHeader = request.getHeader("Stripe-Signature");
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("⚠️ Invalid signature");
        }

        switch (event.getType()) {
            case "payment_intent.succeeded":
                PaymentIntent succeededIntent =
                        (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (succeededIntent != null) {
                    billingHistoryService.updateStatus(
                            succeededIntent.getId(),
                            "succeeded"
                    );
                    System.out.println("💰 Payment succeeded: " + succeededIntent.getId());
                }
                break;

            case "payment_intent.payment_failed":
                PaymentIntent failedIntent =
                        (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (failedIntent != null) {
                    billingHistoryService.updateStatus(
                            failedIntent.getId(),
                            "failed"
                    );
                    System.out.println("❌ Payment failed: " + failedIntent.getId());
                }
                break;

            default:
                System.out.println("⚠️ Unhandled event type: " + event.getType());
        }

        return ResponseEntity.ok("✅ Webhook received");
    }
}
