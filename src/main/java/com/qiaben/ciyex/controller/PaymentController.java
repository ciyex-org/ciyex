package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.PaymentRequestDto;
import com.qiaben.ciyex.dto.GpsPaymentDto;
import com.qiaben.ciyex.entity.PaymentOrder;
import com.qiaben.ciyex.service.GpsPaymentService;
import com.qiaben.ciyex.service.StripeService;
import com.qiaben.ciyex.service.PaymentOrderService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Slf4j
public class PaymentController {

    private final PaymentOrderService orderService;
    private final GpsPaymentService gpsPaymentService;
    private final StripeService stripeService;
    private final com.qiaben.ciyex.service.StripeBillingCardService stripeBillingCardService;

    public PaymentController(PaymentOrderService orderService,
                             GpsPaymentService gpsPaymentService,
                             StripeService stripeService,
                             com.qiaben.ciyex.service.StripeBillingCardService stripeBillingCardService) {
        this.orderService = orderService;
        this.gpsPaymentService = gpsPaymentService;
        this.stripeService = stripeService;
        this.stripeBillingCardService = stripeBillingCardService;
    }

    /**
     * Create a payment order (single or multiple invoices).
     */
    @PostMapping("/create")
    public Map<String, Object> createPayment(@RequestBody PaymentRequestDto request) throws StripeException {
    request.validate();

    // Log full DTO for easier debugging of unexpected field values (e.g., payAll)
    log.debug("createPayment request DTO: {}", request.toString());

    String method = request.getPaymentMethod() != null
        ? request.getPaymentMethod().toUpperCase()
        : "";

    switch (method) {
        case "STRIPE":
        log.info("Creating Stripe payment for org={}, invoices={}, payAll={} -- dto={}",
            request.getOrgId(), request.getInvoiceIds(), request.isPayAll(), request.toString());
        return createStripePayment(request);

            case "GPS":
                log.info("Creating GPS payment for org={}, invoices={}, payAll={}",
                        request.getOrgId(), request.getInvoiceIds(), request.isPayAll());
                return createGpsPayment(request);

            default:
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported payment method: " + request.getPaymentMethod()
                );
        }
    }

    /**
     * Stripe-specific PaymentIntent flow (new SDK safe).
     */
    private Map<String, Object> createStripePayment(PaymentRequestDto request) throws StripeException {
        long amount = orderService.calculateTotalAmount(request);


        PaymentIntent intent;
        String receiptUrl = null;

        // If a saved cardId (payment method) is provided, confirm server-side using saved PM
        if (request.getCardId() != null && !request.getCardId().isBlank()) {
            // cardId is assumed to be a Stripe payment method id; fetch customer id from card record if needed
            String paymentMethodId = request.getCardId();
            String customerId = null;
            try {
                // cardId may be an internal DB id; try to resolve to StripeBillingCard record
                Long cardDbId = null;
                try {
                    cardDbId = Long.parseLong(request.getCardId());
                } catch (NumberFormatException nfe) {
                    // not a numeric id; maybe already a Stripe PM id
                }

                if (cardDbId != null) {
                    var opt = stripeBillingCardService.getById(cardDbId, request.getOrgId());
                    if (opt.isPresent()) {
                        customerId = opt.get().getStripeCustomerId();
                        // if DTO stores Stripe payment method id, prefer it
                        if (opt.get().getStripePaymentMethodId() != null && !opt.get().getStripePaymentMethodId().isBlank()) {
                            paymentMethodId = opt.get().getStripePaymentMethodId();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not resolve stored card to Stripe customer: {}", e.getMessage());
            }

            intent = stripeService.createAndConfirmPaymentIntent(
                    request.getOrgId(), amount, "usd", null, paymentMethodId, customerId, true);

            if (intent.getLatestCharge() != null) {
                receiptUrl = stripeService.getChargeReceiptUrl(request.getOrgId(), intent.getLatestCharge());
            }
        } else {
            // create intent and expand latest_charge so client can confirm with client secret
            intent = stripeService.createPaymentIntent(request.getOrgId(), amount, "usd", null, true);
            if (intent.getLatestCharge() != null) {
                receiptUrl = stripeService.getChargeReceiptUrl(request.getOrgId(), intent.getLatestCharge());
            }
        }

        // Save pending order with receiptUrl
    PaymentOrder order = orderService.createPendingOrder(
        intent.getId(),
        amount,
        "STRIPE",
        request.getCardId(),
        request.isPayAll() ? null : request.getInvoiceIds(),
        receiptUrl,
        request.getOrgId()
    );

        // If the intent is already succeeded (server-confirmed flow), mark order as paid
        String intentStatusNow = intent.getStatus() == null ? "" : intent.getStatus();
        if ("succeeded".equalsIgnoreCase(intentStatusNow)) {
            log.info("PaymentIntent {} already succeeded; marking order {} as PAID", intent.getId(), order.getId());
            try {
                orderService.markOrderAsPaid(intent.getId());
            } catch (Exception e) {
                log.warn("Failed to mark order as paid for intent {}: {}", intent.getId(), e.getMessage());
            }
        }

    Map<String, Object> response = new HashMap<>();
    response.put("paymentMethod", "STRIPE");
    // clientSecret may be null for server-confirmed intents; only include when present
    if (intent.getClientSecret() != null) response.put("clientSecret", intent.getClientSecret());
    response.put("orderId", order.getId());
    response.put("amount", amount);
    response.put("invoiceNumber", order.getInvoiceNumber());
    response.put("receiptUrl", order.getReceiptUrl());
    response.put("payAll", request.isPayAll());

    // Map Stripe PaymentIntent status to a simpler client-friendly status
    String intentStatus = intent.getStatus() == null ? "" : intent.getStatus();
    if ("succeeded".equalsIgnoreCase(intentStatus)) {
        response.put("status", "succeeded");
    } else {
        // anything not succeeded treat as pending on the client
        response.put("status", "PENDING");
    }

    log.info("Stripe intent {} has status={} (mapped to {})", intent.getId(), intentStatus, response.get("status"));

    return response;
    }

    /**
     * GPS-specific payment flow.
     */
    private Map<String, Object> createGpsPayment(PaymentRequestDto request) {
        try {
            GpsPaymentDto gpsPayment = gpsPaymentService.createPaymentForInvoices(request);

            // Save pending order with GPS receiptUrl if provided
        PaymentOrder order = orderService.createPendingOrder(
            gpsPayment.getGpsTransactionId(),
            gpsPayment.getAmount().longValue(),
            "GPS",
            request.getCardId(),
            request.isPayAll() ? null : request.getInvoiceIds(),
            gpsPayment.getReceiptUrl(),
            gpsPayment.getOrgId()
        );

            Map<String, Object> response = new HashMap<>();
            response.put("paymentMethod", "GPS");
            response.put("transactionId", gpsPayment.getGpsTransactionId());
            response.put("amount", gpsPayment.getAmount());
            response.put("status", gpsPayment.getStatus());
            response.put("invoiceNumber", order.getInvoiceNumber());
            response.put("receiptUrl", order.getReceiptUrl());
            response.put("orgId", gpsPayment.getOrgId());
            response.put("userId", gpsPayment.getUserId());
            response.put("cardId", gpsPayment.getCardId());
            response.put("cardRef", gpsPayment.getCardRef());

            return response;
        } catch (Exception e) {
            log.error("GPS payment failed for org={} invoices={}: {}",
                    request.getOrgId(), request.getInvoiceIds(), e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "GPS payment failed: " + e.getMessage()
            );
        }
    }

    /**
     * Fetch PaymentOrder status by ID.
     */
    @GetMapping("/orders/{id}")
    public PaymentOrder getOrderStatus(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found with id " + id
                ));
    }

    /**
     * Convenience endpoint to pay all unpaid invoices for an org.
     */
    @PostMapping("/pay-all")
    public Map<String, Object> payAllInvoices(
            @RequestParam Long orgId,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String cardId
    ) throws StripeException {
        PaymentRequestDto req = new PaymentRequestDto();
        req.setOrgId(orgId);
        req.setPaymentMethod(paymentMethod);
        req.setCardId(cardId);
        req.setPayAll(true);

        // validate and log constructed request before delegating to createPayment
        try {
            req.validate();
        } catch (Exception e) {
            log.error("Invalid pay-all request constructed: {}", req, e);
            throw e;
        }

        log.info("Invoking createPayment via pay-all helper with request={}", req.toString());

        return createPayment(req);
    }
}
