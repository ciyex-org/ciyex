package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.StripeBillingHistoryDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.integration.StripeConfig;
import com.qiaben.ciyex.entity.StripeBillingHistory;
import com.qiaben.ciyex.entity.InvoiceBill;
import com.qiaben.ciyex.entity.InvoiceStatus;
import com.qiaben.ciyex.repository.StripeBillingHistoryRepository;
import com.qiaben.ciyex.repository.InvoiceBillRepository;
import com.qiaben.ciyex.repository.StripeBillingCardRepository;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StripeBillingHistoryService {

    private final StripeBillingHistoryRepository repo;
    private final InvoiceBillRepository invoiceRepo;
    private final StripeBillingCardRepository stripeCardRepo;
    private final OrgIntegrationConfigProvider configProvider;

    public StripeBillingHistoryService(
            StripeBillingHistoryRepository repo,
            InvoiceBillRepository invoiceRepo,
            StripeBillingCardRepository stripeCardRepo,
            OrgIntegrationConfigProvider configProvider
    ) {
        this.repo = repo;
        this.invoiceRepo = invoiceRepo;
        this.stripeCardRepo = stripeCardRepo;
        this.configProvider = configProvider;
    }

    private Long requireOrg(String op) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new SecurityException("No orgId in RequestContext during " + op);
        return orgId;
    }

    /* ------------------- PAY NOW ------------------- */
    @Transactional
    public StripeBillingHistoryDto recordPayment(StripeBillingHistoryDto dto) {
        Long orgId = requireOrg("recordPayment");
        dto.setOrgId(orgId);

        StripeConfig stripeCfg = configProvider.getStripeForCurrentOrg();
        if (stripeCfg == null || stripeCfg.getApiKey() == null) {
            throw new RuntimeException("Stripe is not configured for this org");
        }
        Stripe.apiKey = stripeCfg.getApiKey();

        try {
            String paymentMethodId = dto.getStripePaymentMethodId();
            if (paymentMethodId == null || paymentMethodId.isBlank()) {
                paymentMethodId = stripeCardRepo.findFirstByUserIdAndOrgIdAndIsDefaultTrue(dto.getUserId(), orgId)
                        .map(c -> c.getStripePaymentMethodId())
                        .orElseThrow(() -> new RuntimeException(
                                "No default Stripe card found for user " + dto.getUserId()));
            }

            PaymentIntent intent;
            if (paymentMethodId.startsWith("pi_")) {
                intent = PaymentIntent.retrieve(paymentMethodId).confirm();
            } else {
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount((long) (dto.getAmount() * 100))
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
                    .receiptUrl(receiptUrl)
                    .externalId("INV-" + System.currentTimeMillis())
                    .invoiceUrl(null)
                    .build();

            invoice = invoiceRepo.save(invoice);

            StripeBillingHistory entity = StripeBillingHistory.builder()
                    .orgId(orgId)
                    .userId(dto.getUserId())
                    .stripePaymentIntentId(intent.getId())
                    .stripePaymentMethodId(paymentMethodId)
                    .amount(dto.getAmount())
                    .status(normalizeStripeStatus(intent.getStatus()))
                    .invoiceBill(invoice)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return toDto(repo.save(entity));

        } catch (StripeException e) {
            throw new RuntimeException("Stripe payment failed: " + e.getMessage(), e);
        }
    }

    /* ------------------- ARCHIVE / UNARCHIVE ------------------- */
    @Transactional
    public StripeBillingHistoryDto archive(Long id) {
        StripeBillingHistory entity = repo.findById(id).orElseThrow(() -> new RuntimeException("Stripe billing history not found"));
        entity.setStatus("ARCHIVED");
        entity.setUpdatedAt(LocalDateTime.now());
        return toDto(repo.save(entity));
    }

    @Transactional
    public StripeBillingHistoryDto unarchive(Long id) {
        StripeBillingHistory entity = repo.findById(id).orElseThrow(() -> new RuntimeException("Stripe billing history not found"));
        if ("ARCHIVED".equalsIgnoreCase(entity.getStatus())) {
            entity.setStatus("SUCCEEDED");
            entity.setUpdatedAt(LocalDateTime.now());
        }
        return toDto(repo.save(entity));
    }

    /* ------------------- DELETE ------------------- */
    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    /* ------------------- GET ------------------- */
    @Transactional(readOnly = true)
    public List<StripeBillingHistoryDto> getByUser(Long userId) {
        Long orgId = requireOrg("getByUser");
        return repo.findByUserIdAndOrgId(userId, orgId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StripeBillingHistoryDto> getAll() {
        Long orgId = requireOrg("getAll");
        return repo.findByOrgId(orgId).stream().map(this::toDto).collect(Collectors.toList());
    }

    /* ------------------- UPDATE STATUS (Webhook) ------------------- */
    @Transactional
    public void updateStatus(String paymentIntentId, String stripeStatus) {
        repo.findByStripePaymentIntentId(paymentIntentId).ifPresentOrElse(entity -> {
            String normalized = normalizeStripeStatus(stripeStatus);
            entity.setStatus(normalized);
            entity.setUpdatedAt(LocalDateTime.now());

            if (entity.getInvoiceBill() != null) {
                entity.getInvoiceBill().setStatus(
                        "SUCCEEDED".equals(normalized) ? InvoiceStatus.PAID : InvoiceStatus.PENDING
                );
                invoiceRepo.save(entity.getInvoiceBill());
            }

            repo.save(entity);
            log.info("✅ Updated StripeBillingHistory {} to {}", paymentIntentId, normalized);
        }, () -> log.warn("⚠️ No StripeBillingHistory found for PaymentIntent {}", paymentIntentId));
    }

    /* ------------------- HELPER ------------------- */
    private String normalizeStripeStatus(String raw) {
        if (raw == null) return "PENDING";
        switch (raw.toLowerCase()) {
            case "succeeded": return "SUCCEEDED";
            case "requires_payment_method":
            case "requires_action":
            case "requires_confirmation": return "PENDING";
            case "processing": return "PROCESSING";
            case "canceled": return "CANCELED";
            case "failed":
            case "payment_failed": return "FAILED";
            default: return raw.toUpperCase();
        }
    }

    private StripeBillingHistoryDto toDto(StripeBillingHistory r) {
        return StripeBillingHistoryDto.builder()
                .id(r.getId())
                .orgId(r.getOrgId())
                .userId(r.getUserId())
                .stripePaymentIntentId(r.getStripePaymentIntentId())
                .stripePaymentMethodId(r.getStripePaymentMethodId())
                .amount(r.getAmount())
                .status(r.getStatus())
                .invoiceBillId(r.getInvoiceBill() != null ? r.getInvoiceBill().getId() : null)
                .externalId(r.getInvoiceBill() != null ? r.getInvoiceBill().getExternalId() : null)
                .invoiceUrl(r.getInvoiceBill() != null ? r.getInvoiceBill().getInvoiceUrl() : null)
                .receiptUrl(r.getInvoiceBill() != null ? r.getInvoiceBill().getReceiptUrl() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
