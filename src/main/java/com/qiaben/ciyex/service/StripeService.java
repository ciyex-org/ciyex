package com.qiaben.ciyex.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiaben.ciyex.dto.integration.StripeConfig;
import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.repository.OrgConfigRepository;
import com.stripe.net.RequestOptions;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    private final OrgConfigRepository orgConfigRepository;
    private final ObjectMapper objectMapper;

    /**
     * Load StripeConfig for the given org.
     */
    private StripeConfig loadStripeConfig(Long orgId) {
        Optional<OrgConfig> maybe = orgConfigRepository.findByOrgId(orgId);
        if (maybe.isEmpty()) {
            throw new IllegalStateException("OrgConfig not found for org=" + orgId);
        }

        OrgConfig orgConfig = maybe.get();
        if (orgConfig.getIntegrations() == null || orgConfig.getIntegrations().get("stripe") == null) {
            throw new IllegalStateException("Stripe configuration missing for org=" + orgId);
        }

        return objectMapper.convertValue(orgConfig.getIntegrations().get("stripe"), StripeConfig.class);
    }

    /**
     * Retrieve a PaymentIntent and return the receipt URL for its latest charge (if available).
     */
    public String fetchReceiptUrlForPaymentIntent(Long orgId, String paymentIntentId) throws StripeException {
        StripeConfig stripeConfig = loadStripeConfig(orgId);
        if (stripeConfig.getApiKey() == null) {
            throw new IllegalStateException("Stripe apiKey not configured for org=" + orgId);
        }

        RequestOptions requestOptions = RequestOptions.builder().setApiKey(stripeConfig.getApiKey()).build();
        PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId, requestOptions);
        if (intent == null || intent.getLatestCharge() == null) return null;
        Charge charge = Charge.retrieve(intent.getLatestCharge(), requestOptions);
        return charge == null ? null : charge.getReceiptUrl();
    }

    /**
     * Public helper to get publishableKey for frontend initialization. Returns null when not configured.
     */
    public String getPublishableKey(Long orgId) {
        try {
            StripeConfig cfg = loadStripeConfig(orgId);
            return cfg == null ? null : cfg.getPublishableKey();
        } catch (Exception e) {
            log.debug("No stripe config for org={}", orgId);
            return null;
        }
    }

    /**
     * Create a PaymentIntent for the given org.
     */
    public PaymentIntent createPaymentIntent(Long orgId, long amountCents, String currency, String description) throws StripeException {
        return createPaymentIntent(orgId, amountCents, currency, description, false);
    }


    /**
     * Create a PaymentIntent for the given org with optional expand of latest_charge.
     */
    public PaymentIntent createPaymentIntent(Long orgId, long amountCents, String currency, String description, boolean expandLatestCharge) throws StripeException {
        StripeConfig stripeConfig = loadStripeConfig(orgId);
        if (stripeConfig.getApiKey() == null) {
            throw new IllegalStateException("Stripe apiKey not configured for org=" + orgId);
        }

        // Use per-request RequestOptions so we don't change the global key (thread-safe/multi-tenant)
        RequestOptions requestOptions = RequestOptions.builder().setApiKey(stripeConfig.getApiKey()).build();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("amount", amountCents);
        params.put("currency", currency);
        if (description != null) {
            params.put("description", description);
        }
        Map<String, Object> autoPay = Collections.singletonMap("enabled", Boolean.TRUE);
        params.put("automatic_payment_methods", autoPay);
        if (expandLatestCharge) {
            params.put("expand", Arrays.asList("latest_charge"));
        }

        PaymentIntent intent = PaymentIntent.create(params, requestOptions);

        log.info("Created PaymentIntent={} for org={}", intent.getId(), orgId);
        return intent;
    }

    /**
     * Retrieve a Charge and return its receipt URL using the org's API key.
     */
    public String getChargeReceiptUrl(Long orgId, String chargeId) throws StripeException {
        StripeConfig stripeConfig = loadStripeConfig(orgId);
        if (stripeConfig.getApiKey() == null) {
            throw new IllegalStateException("Stripe apiKey not configured for org=" + orgId);
        }

        RequestOptions requestOptions = RequestOptions.builder().setApiKey(stripeConfig.getApiKey()).build();
        Charge charge = Charge.retrieve(chargeId, requestOptions);
        return charge == null ? null : charge.getReceiptUrl();
    }

    /**
     * Create and confirm a PaymentIntent using a saved payment method (server-side confirmation).
     * Useful for charging saved cards (payment method id available).
     */
    public PaymentIntent createAndConfirmPaymentIntent(Long orgId,
                                                       long amountCents,
                                                       String currency,
                                                       String description,
                                                       String paymentMethodId,
                                                       String customerId,
                                                       boolean offSession) throws StripeException {
        StripeConfig stripeConfig = loadStripeConfig(orgId);
        if (stripeConfig.getApiKey() == null) {
            throw new IllegalStateException("Stripe apiKey not configured for org=" + orgId);
        }

        RequestOptions requestOptions = RequestOptions.builder().setApiKey(stripeConfig.getApiKey()).build();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("amount", amountCents);
        params.put("currency", currency);
        if (description != null) params.put("description", description);
        if (paymentMethodId != null) params.put("payment_method", paymentMethodId);
        if (customerId != null) params.put("customer", customerId);
        params.put("confirm", Boolean.TRUE);
        if (offSession) params.put("off_session", Boolean.TRUE);

        PaymentIntent intent = PaymentIntent.create(params, requestOptions);
        log.info("Created+confirmed PaymentIntent={} for org={} via pm={}", intent.getId(), orgId, paymentMethodId);
        return intent;
    }

    /**
     * Verify Stripe webhook event using org-specific secret.
     */
    public Event verifyWebhook(Long orgId, String payload, String sigHeader) throws SignatureVerificationException {
        StripeConfig stripeConfig = loadStripeConfig(orgId);
        if (stripeConfig.getWebhookSecret() == null) {
            throw new IllegalStateException("Stripe webhookSecret not configured for org=" + orgId);
        }
        return Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
    }
}
