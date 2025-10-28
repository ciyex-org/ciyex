package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.BillingHistoryDto;
import com.qiaben.ciyex.dto.integration.GpsConfig;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.integration.StripeConfig;
import com.qiaben.ciyex.entity.*;
import com.qiaben.ciyex.entity.BillingHistory.BillingProvider;
import com.qiaben.ciyex.entity.BillingHistory.BillingStatus;
import com.qiaben.ciyex.repository.*;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BillingHistoryService {

    private final BillingHistoryRepository billingRepo;
    private final InvoiceBillRepository invoiceRepo;
    private final StripeBillingCardRepository stripeCardRepo;
    private final GpsBillingCardRepository gpsCardRepo;
    private final OrgIntegrationConfigProvider configProvider;

    private Long requireOrg(String operation) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) {
            throw new RuntimeException("Organization context required for " + operation);
        }
        return ctx.getOrgId();
    }

    /* ------------------- STRIPE PAYMENT ------------------- */
    @Transactional
    public BillingHistoryDto recordStripePayment(BillingHistoryDto dto) {
        Long orgId = requireOrg("recordStripePayment");
        dto.setOrgId(orgId);
        dto.setProvider(BillingProvider.STRIPE);

        StripeConfig stripeCfg = configProvider.getStripeForCurrentOrg();
        if (stripeCfg == null || stripeCfg.getApiKey() == null) {
            throw new RuntimeException("Stripe is not configured for this org");
        }
        Stripe.apiKey = stripeCfg.getApiKey();

        try {
            String paymentMethodId = dto.getStripePaymentMethodId();
            if (paymentMethodId == null || paymentMethodId.isBlank()) {
                paymentMethodId = stripeCardRepo.findFirstByUserIdAndOrgIdAndIsDefaultTrue(dto.getUserId(), orgId)
                        .map(StripeBillingCard::getStripePaymentMethodId)
                        .orElseThrow(() -> new RuntimeException("No default Stripe card found for user " + dto.getUserId()));
            }

            PaymentIntent intent;
            if (paymentMethodId.startsWith("pi_")) {
                intent = PaymentIntent.retrieve(paymentMethodId).confirm();
            } else {
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount((long) (dto.getAmount().doubleValue() * 100))
                        .setCurrency("usd")
                        .setPaymentMethod(paymentMethodId)
                        .setConfirm(true)
                        .setOffSession(true)
                        .build();
                intent = PaymentIntent.create(params);
            }

            String receiptUrl = null;
            if (intent.getLatestCharge() != null) {
                try {
                    Charge charge = Charge.retrieve(intent.getLatestCharge());
                    receiptUrl = charge.getReceiptUrl();
                } catch (StripeException se) {
                    log.warn("Could not fetch receipt for PaymentIntent {}: {}", intent.getId(), se.getMessage());
                }
            }

        InvoiceBill invoice = InvoiceBill.builder()
            .orgId(orgId)
            .userId(dto.getUserId())
            .amount(dto.getAmount())
            .status("succeeded".equalsIgnoreCase(intent.getStatus()) ? InvoiceStatus.PAID : InvoiceStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .externalId("INV-" + System.currentTimeMillis())
            .receiptUrl(receiptUrl)
            .build();
            invoice = invoiceRepo.save(invoice);

        BillingHistory entity = BillingHistory.builder()
                    .orgId(orgId)
                    .userId(dto.getUserId())
                    .provider(BillingProvider.STRIPE)
                    .stripePaymentIntentId(intent.getId())
                    .stripePaymentMethodId(paymentMethodId)
            .amount(dto.getAmount())
                    .status(normalizeStripeStatus(intent.getStatus()))
                    .invoiceBill(invoice)
                    .invoiceUrl(invoice.getInvoiceUrl())
                    .receiptUrl(invoice.getReceiptUrl())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return toDto(billingRepo.save(entity));

        } catch (StripeException e) {
            throw new RuntimeException("Stripe payment failed: " + e.getMessage(), e);
        }
    }

    /* ------------------- GPS PAYMENT ------------------- */
    @Transactional
    public BillingHistoryDto recordGpsPayment(BillingHistoryDto dto) {
        Long orgId = requireOrg("recordGpsPayment");
        dto.setOrgId(orgId);
        dto.setProvider(BillingProvider.GPS);

        try {
            String transactionId = "GPS-" + System.currentTimeMillis();

        InvoiceBill invoice = InvoiceBill.builder()
            .orgId(orgId)
            .userId(dto.getUserId())
            .amount(dto.getAmount())
            .status(InvoiceStatus.PAID)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .externalId("GPS-INV-" + System.currentTimeMillis())
            .receiptUrl("gps-receipt-" + transactionId + ".pdf")
            .build();
            invoice = invoiceRepo.save(invoice);

        BillingHistory entity = BillingHistory.builder()
                    .orgId(orgId)
                    .userId(dto.getUserId())
                    .provider(BillingProvider.GPS)
                    .gpsTransactionId(transactionId)
                    .gpsCustomerVaultId(dto.getGpsCustomerVaultId())
            .amount(dto.getAmount())
                    .status(BillingStatus.SUCCEEDED)
                    .responseMessage("Payment processed successfully")
                    .invoiceBill(invoice)
                    .invoiceUrl(invoice.getInvoiceUrl())
                    .receiptUrl(invoice.getReceiptUrl())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return toDto(billingRepo.save(entity));

        } catch (Exception e) {
            throw new RuntimeException("GPS payment failed: " + e.getMessage(), e);
        }
    }

    /* ------------------- HELPERS ------------------- */
    private BillingStatus normalizeStripeStatus(String raw) {
        if (raw == null) return BillingStatus.PENDING;
        switch (raw.toLowerCase()) {
            case "succeeded":
                return BillingStatus.SUCCEEDED;
            case "processing":
                return BillingStatus.PROCESSING;
            case "canceled":
                return BillingStatus.CANCELED;
            case "failed":
            case "payment_failed":
                return BillingStatus.FAILED;
            default:
                return BillingStatus.PENDING;
        }
    }

    private BillingHistoryDto toDto(BillingHistory entity) {
        return BillingHistoryDto.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .userId(entity.getUserId())
                .provider(entity.getProvider())
                .stripePaymentIntentId(entity.getStripePaymentIntentId())
                .stripePaymentMethodId(entity.getStripePaymentMethodId())
                .gpsTransactionId(entity.getGpsTransactionId())
                .gpsCustomerVaultId(entity.getGpsCustomerVaultId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .responseMessage(entity.getResponseMessage())
                .responseCode(entity.getResponseCode() != null ? entity.getResponseCode().toString() : null)
                .invoiceBillId(entity.getInvoiceBill() != null ? entity.getInvoiceBill().getId() : null)
                .externalId(entity.getInvoiceBill() != null ? entity.getInvoiceBill().getExternalId() : null)
                .invoiceUrl(entity.getInvoiceBill() != null ? entity.getInvoiceBill().getInvoiceUrl() : null)
                .receiptUrl(entity.getInvoiceBill() != null ? entity.getInvoiceBill().getReceiptUrl() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /* ------------ Query/Management helpers ------------ */
    public java.util.List<BillingHistoryDto> getAll() {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) throw new RuntimeException("Org context required");
        Long orgId = ctx.getOrgId();
        return billingRepo.findByOrgIdOrderByCreatedAtDesc(orgId).stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<BillingHistoryDto> getByUser(Long userId) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) throw new RuntimeException("Org context required");
        Long orgId = ctx.getOrgId();
        return billingRepo.findByOrgIdAndUserIdOrderByCreatedAtDesc(orgId, userId).stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void delete(Long id) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) throw new RuntimeException("Org context required");
        Long orgId = ctx.getOrgId();
        BillingHistory h = billingRepo.findById(id).orElseThrow(() -> new RuntimeException("Billing history not found"));
        if (!h.getOrgId().equals(orgId)) throw new RuntimeException("Access denied");
        billingRepo.delete(h);
    }

    @Transactional
    public void archive(Long id) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) throw new RuntimeException("Org context required");
        Long orgId = ctx.getOrgId();
        BillingHistory h = billingRepo.findById(id).orElseThrow(() -> new RuntimeException("Billing history not found"));
        if (!h.getOrgId().equals(orgId)) throw new RuntimeException("Access denied");
        h.setStatus(BillingStatus.ARCHIVED);
        h.setUpdatedAt(java.time.LocalDateTime.now());
        billingRepo.save(h);
    }

    @Transactional
    public void unarchive(Long id) {
        RequestContext ctx = RequestContext.get();
        if (ctx == null || ctx.getOrgId() == null) throw new RuntimeException("Org context required");
        Long orgId = ctx.getOrgId();
        BillingHistory h = billingRepo.findById(id).orElseThrow(() -> new RuntimeException("Billing history not found"));
        log.info("unarchive called for billingHistory id={}, orgId={}, currentStatus={}", id, orgId, h.getStatus());
        if (!h.getOrgId().equals(orgId)) throw new RuntimeException("Access denied");
        // Only unarchive if it's currently archived. Do not convert pending/unpaid -> succeeded.
        if (h.getStatus() == BillingStatus.ARCHIVED) {
            // If linked invoice exists, use its status to infer billing status
            if (h.getInvoiceBill() != null && h.getInvoiceBill().getStatus() != null) {
                // Map InvoiceStatus -> BillingStatus
                switch (h.getInvoiceBill().getStatus()) {
                    case PAID:
                        h.setStatus(BillingStatus.SUCCEEDED);
                        break;
                    case UNPAID:
                    case PENDING:
                    case CANCELLED:
                    default:
                        h.setStatus(BillingStatus.PENDING);
                        break;
                }
            } else {
                // No linked invoice; default to PENDING
                h.setStatus(BillingStatus.PENDING);
            }
            h.setUpdatedAt(LocalDateTime.now());
            billingRepo.save(h);
            log.info("unarchived billingHistory id={} -> status={}", id, h.getStatus());
        }
    }

    @Transactional
    public void updateStatus(String externalId, String rawStatus) {
        // Try Stripe
        Optional<BillingHistory> byStripe = billingRepo.findByStripePaymentIntentIdAndProvider(externalId, BillingProvider.STRIPE);
        if (byStripe.isPresent()) {
            BillingHistory h = byStripe.get();
            h.setStatus(normalizeStripeStatus(rawStatus));
            h.setUpdatedAt(LocalDateTime.now());
            billingRepo.save(h);
            return;
        }
        // Try GPS
        Optional<BillingHistory> byGps = billingRepo.findByGpsTransactionIdAndProvider(externalId, BillingProvider.GPS);
        if (byGps.isPresent()) {
            BillingHistory h = byGps.get();
            h.setStatus(normalizeStripeStatus(rawStatus));
            h.setUpdatedAt(LocalDateTime.now());
            billingRepo.save(h);
            return;
        }
        // no-op if not found
    }
}
