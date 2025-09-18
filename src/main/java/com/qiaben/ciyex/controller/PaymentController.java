package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.entity.PaymentOrder;
import com.qiaben.ciyex.service.PaymentOrderService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe")
public class PaymentController {

    private final PaymentOrderService orderService;

    public PaymentController(PaymentOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new Stripe PaymentIntent and a matching PaymentOrder in DB
     */
    @PostMapping("/create-payment-intent")
    public Map<String, Object> createPaymentIntent(@RequestBody Map<String, Object> request) throws StripeException {
        Long amount = Long.valueOf(request.get("amount").toString()); // cents

        // 🔹 Step 1: Create Stripe PaymentIntent
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", "usd");
        params.put("automatic_payment_methods", Map.of("enabled", true));

        PaymentIntent intent = PaymentIntent.create(params);

        // 🔹 Step 2: Save PaymentOrder in DB with PENDING status
        PaymentOrder order = orderService.createPendingOrder(intent.getId(), amount);

        // 🔹 Step 3: Return clientSecret for Stripe.js and DB orderId for tracking
        return Map.of(
                "clientSecret", intent.getClientSecret(),
                "orderId", order.getId()
        );
    }

    /**
     * Optional: Fetch PaymentOrder status by ID
     */
    @GetMapping("/orders/{id}")
    public PaymentOrder getOrderStatus(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id " + id));
    }
}
