package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.BillingHistoryDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.dto.integration.StripeConfig;
import com.qiaben.ciyex.entity.BillingCard;
import com.qiaben.ciyex.entity.BillingHistory;
import com.qiaben.ciyex.entity.InvoiceBill;
import com.qiaben.ciyex.entity.InvoiceStatus;
import com.qiaben.ciyex.repository.BillingCardRepository;
import com.qiaben.ciyex.repository.BillingHistoryRepository;
import com.qiaben.ciyex.repository.InvoiceBillRepository;
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
public class BillingHistoryService {

    private final BillingHistoryRepository repo;
    private final InvoiceBillRepository invoiceRepo;
    private final BillingCardRepository cardRepo;
    private final OrgIntegrationConfigProvider configProvider;

    public BillingHistoryService(
            BillingHistoryRepository repo,
            InvoiceBillRepository invoiceRepo,
            BillingCardRepository cardRepo,
            OrgIntegrationConfigProvider configProvider
    ) {
        this.repo = repo;
        this.invoiceRepo = invoiceRepo;
        this.cardRepo = cardRepo;
        this.configProvider = configProvider;
    }

    private Long requireOrg(String op) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new SecurityException("No orgId in RequestContext during " + op);
        return orgId;
    }

    /* ------------------- PAY NOW ------------------- */
    @Transactional
    public BillingHistoryDto recordPayment(BillingHistoryDto dto) {
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
                BillingCard defaultCard = cardRepo.findFirstByUserIdAndOrgIdAndIsDefaultTrue(dto.getUserId(), orgId)
                        .orElseThrow(() -> new RuntimeException("No default billing card found for user " + dto.getUserId()));
                paymentMethodId = defaultCard.getStripePaymentMethodId();
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
                    .build();

            invoice = invoiceRepo.save(invoice);

            BillingHistory entity = BillingHistory.builder()
                    .orgId(orgId)
                    .userId(dto.getUserId())
                    .stripePaymentIntentId(intent.getId())
                    .stripePaymentMethodId(paymentMethodId)
                    .amount(dto.getAmount())
                    .status(intent.getStatus().toUpperCase())
                    .invoiceBill(invoice)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return toDto(repo.save(entity));

        } catch (StripeException e) {
            throw new RuntimeException("Stripe payment failed: " + e.getMessage(), e);
        }
    }

    /* ------------------- UPDATE STATUS (for Webhooks) ------------------- */
    @Transactional
    public void updateStatus(String paymentIntentId, String status) {
        repo.findByStripePaymentIntentId(paymentIntentId).ifPresentOrElse(entity -> {
            entity.setStatus(status.toUpperCase());
            entity.setUpdatedAt(LocalDateTime.now());

            if (entity.getInvoiceBill() != null) {
                entity.getInvoiceBill().setStatus(
                        "succeeded".equalsIgnoreCase(status)
                                ? InvoiceStatus.PAID
                                : InvoiceStatus.PENDING
                );
                invoiceRepo.save(entity.getInvoiceBill());
            }

            repo.save(entity);
            log.info("Updated BillingHistory {} to status {}", paymentIntentId, status);
        }, () -> log.warn("No BillingHistory found for PaymentIntent {}", paymentIntentId));
    }

    /* ------------------- DELETE ------------------- */
    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    /* ------------------- ARCHIVE / UNARCHIVE ------------------- */
    @Transactional
    public BillingHistoryDto archive(Long id) {
        BillingHistory entity = repo.findById(id).orElseThrow(() -> new RuntimeException("Billing history not found"));
        entity.setStatus("ARCHIVED");
        entity.setUpdatedAt(LocalDateTime.now());
        return toDto(repo.save(entity));
    }

    @Transactional
    public BillingHistoryDto unarchive(Long id) {
        BillingHistory entity = repo.findById(id).orElseThrow(() -> new RuntimeException("Billing history not found"));
        if ("ARCHIVED".equalsIgnoreCase(entity.getStatus())) {
            entity.setStatus("SUCCEEDED");
            entity.setUpdatedAt(LocalDateTime.now());
        }
        return toDto(repo.save(entity));
    }

    /* ------------------- GET ------------------- */
    @Transactional(readOnly = true)
    public List<BillingHistoryDto> getByUser(Long userId) {
        Long orgId = requireOrg("getByUser");
        return repo.findByUserIdAndOrgId(userId, orgId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BillingHistoryDto> getAll() {
        Long orgId = requireOrg("getAll");
        return repo.findByOrgId(orgId).stream().map(this::toDto).collect(Collectors.toList());
    }

    /* ------------------- MAPPER ------------------- */
    private BillingHistoryDto toDto(BillingHistory r) {
        return BillingHistoryDto.builder()
                .id(r.getId())
                .orgId(r.getOrgId())
                .userId(r.getUserId())
                .stripePaymentIntentId(r.getStripePaymentIntentId())
                .stripePaymentMethodId(r.getStripePaymentMethodId())
                .amount(r.getAmount())
                .status(r.getStatus())
                .invoiceBillId(r.getInvoiceBill() != null ? r.getInvoiceBill().getId() : null)
                .invoiceUrl(r.getInvoiceBill() != null ? r.getInvoiceBill().getInvoiceUrl() : null)
                .receiptUrl(r.getInvoiceBill() != null ? r.getInvoiceBill().getReceiptUrl() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
