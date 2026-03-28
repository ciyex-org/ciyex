package org.ciyex.ehr.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.payment.dto.PatientLedgerDto;
import org.ciyex.ehr.payment.dto.PaymentPlanDto;
import org.ciyex.ehr.payment.dto.PaymentTransactionDto;
import org.ciyex.ehr.payment.entity.PatientLedger;
import org.ciyex.ehr.payment.entity.PaymentPlan;
import org.ciyex.ehr.payment.entity.PaymentTransaction;
import org.ciyex.ehr.payment.repository.PatientLedgerRepository;
import org.ciyex.ehr.payment.repository.PaymentPlanRepository;
import org.ciyex.ehr.payment.repository.PaymentTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository transactionRepo;
    private final PaymentPlanRepository planRepo;
    private final PatientLedgerRepository ledgerRepo;
    private final org.ciyex.ehr.util.OrgIntegrationConfigProvider configProvider;

    private String orgAlias() {
        return RequestContext.get().getOrgName();
    }

    // ── Stripe PaymentIntent ──────────────────────────────────────────────

    /**
     * Create a Stripe PaymentIntent or return a mock intent if Stripe is not configured.
     */
    public Map<String, Object> createPaymentIntent(Map<String, Object> body) {
        long amountCents = Math.round(Double.parseDouble(body.get("amount").toString()) * 100);
        String currency = body.getOrDefault("currency", "usd").toString();
        String description = body.getOrDefault("description", "Patient payment").toString();

        var stripeConfig = configProvider.getStripeConfig();

        if (stripeConfig != null && stripeConfig.getApiKey() != null && !stripeConfig.getApiKey().isBlank()) {
            // Real Stripe mode
            try {
                com.stripe.Stripe.apiKey = stripeConfig.getApiKey();
                var params = com.stripe.param.PaymentIntentCreateParams.builder()
                        .setAmount(amountCents)
                        .setCurrency(currency)
                        .setDescription(description)
                        .setAutomaticPaymentMethods(
                                com.stripe.param.PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();
                var intent = com.stripe.model.PaymentIntent.create(params);
                return Map.of(
                        "clientSecret", intent.getClientSecret(),
                        "paymentIntentId", intent.getId(),
                        "publishableKey", stripeConfig.getPublishableKey() != null ? stripeConfig.getPublishableKey() : "",
                        "mode", "live"
                );
            } catch (com.stripe.exception.StripeException e) {
                log.error("Stripe PaymentIntent creation failed", e);
                throw new RuntimeException("Stripe error: " + e.getMessage());
            }
        }

        // Demo/mock mode — no Stripe configured
        String mockId = "pi_mock_" + System.currentTimeMillis();
        return Map.of(
                "clientSecret", mockId + "_secret_mock",
                "paymentIntentId", mockId,
                "publishableKey", "",
                "mode", "demo"
        );
    }

    /**
     * Get Stripe publishable config for frontend.
     */
    public Map<String, Object> getStripePublishableConfig() {
        var stripeConfig = configProvider.getStripeConfig();
        if (stripeConfig != null && stripeConfig.getPublishableKey() != null && !stripeConfig.getPublishableKey().isBlank()) {
            return Map.of(
                    "configured", true,
                    "publishableKey", stripeConfig.getPublishableKey()
            );
        }
        return Map.of("configured", false);
    }

    // ── Transactions ─────────────────────────────────────────────────────

    @Transactional
    public PaymentTransactionDto collectPayment(PaymentTransactionDto dto) {
        // Validate required fields
        if (dto.getPatientId() == null) {
            throw new IllegalArgumentException("Patient is required");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A valid payment amount is required");
        }
        if (dto.getPaymentMethodType() == null || dto.getPaymentMethodType().isBlank()) {
            throw new IllegalArgumentException("Payment method type is required");
        }
        // For card payments, require a saved payment method
        if (("credit_card".equals(dto.getPaymentMethodType()) || "debit_card".equals(dto.getPaymentMethodType()))
                && dto.getPaymentMethodId() == null) {
            throw new IllegalArgumentException("A saved payment method is required for card payments");
        }

        var txn = PaymentTransaction.builder()
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .paymentMethodId(dto.getPaymentMethodId())
                .amount(dto.getAmount())
                .currency(dto.getCurrency() != null ? dto.getCurrency() : "USD")
                .status("completed")  // mock: mark completed immediately
                .transactionType("payment")
                .paymentMethodType(dto.getPaymentMethodType())
                .cardBrand(dto.getCardBrand())
                .lastFour(dto.getLastFour())
                .description(dto.getDescription())
                .referenceType(dto.getReferenceType())
                .referenceId(dto.getReferenceId())
                .invoiceNumber(dto.getInvoiceNumber())
                .convenienceFee(dto.getConvenienceFee() != null ? dto.getConvenienceFee() : BigDecimal.ZERO)
                .collectedBy(dto.getCollectedBy())
                .collectedAt(LocalDateTime.now())
                .receiptEmail(dto.getReceiptEmail())
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();

        txn = transactionRepo.save(txn);

        // Add ledger entry for this payment (negative amount = reduces balance)
        var ledgerEntry = postPayment(dto.getPatientId(), dto.getAmount(),
                dto.getDescription() != null ? dto.getDescription() : "Payment collected",
                txn.getId());
        // Populate ledger with invoice/recipient/issuer from the transaction
        var ledger = ledgerRepo.findById(ledgerEntry.getId()).orElse(null);
        if (ledger != null) {
            ledger.setInvoiceNumber(dto.getInvoiceNumber());
            ledger.setRecipient(dto.getPatientName());
            ledger.setIssuer(orgAlias());
            ledgerRepo.save(ledger);
        }

        return toTransactionDto(txn);
    }

    @Transactional(readOnly = true)
    public PaymentTransactionDto getTransaction(Long id) {
        return transactionRepo.findByIdAndOrgAlias(id, orgAlias())
                .map(this::toTransactionDto)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<PaymentTransactionDto> listTransactions(Pageable pageable) {
        return transactionRepo.findByOrgAliasOrderByCreatedAtDesc(orgAlias(), pageable)
                .map(this::toTransactionDto);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getByPatient(Long patientId) {
        return transactionRepo.findByOrgAliasAndPatientIdOrderByCreatedAtDesc(orgAlias(), patientId)
                .stream().map(this::toTransactionDto).toList();
    }

    @Transactional
    public PaymentTransactionDto refund(Long transactionId, BigDecimal amount, String reason) {
        var txn = transactionRepo.findByIdAndOrgAlias(transactionId, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + transactionId));

        if (!"completed".equals(txn.getStatus()) && !"partial_refund".equals(txn.getStatus())) {
            throw new IllegalStateException("Cannot refund transaction with status: " + txn.getStatus());
        }

        BigDecimal refundable = txn.getAmount().subtract(txn.getRefundAmount());
        if (amount.compareTo(refundable) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds refundable balance. Max: " + refundable);
        }

        txn.setRefundAmount(txn.getRefundAmount().add(amount));
        txn.setRefundReason(reason);

        if (txn.getRefundAmount().compareTo(txn.getAmount()) >= 0) {
            txn.setStatus("refunded");
        } else {
            txn.setStatus("partial_refund");
        }

        txn = transactionRepo.save(txn);

        // Add ledger entry for refund (positive amount = increases balance)
        addLedgerEntry(txn.getPatientId(), "refund", amount,
                "Refund: " + (reason != null ? reason : ""),
                "payment_transaction", txn.getId());

        return toTransactionDto(txn);
    }

    @Transactional
    public PaymentTransactionDto updateTransaction(Long transactionId, PaymentTransactionDto dto) {
        var txn = transactionRepo.findByIdAndOrgAlias(transactionId, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + transactionId));

        if ("voided".equals(txn.getStatus()) || "refunded".equals(txn.getStatus())) {
            throw new IllegalStateException("Cannot update transaction with status: " + txn.getStatus());
        }

        if (dto.getAmount() != null) txn.setAmount(dto.getAmount());
        if (dto.getDescription() != null) txn.setDescription(dto.getDescription());
        if (dto.getPaymentMethodType() != null) txn.setPaymentMethodType(dto.getPaymentMethodType());
        if (dto.getNotes() != null) txn.setNotes(dto.getNotes());

        txn = transactionRepo.save(txn);
        return toTransactionDto(txn);
    }

    @Transactional
    public PaymentTransactionDto voidTransaction(Long transactionId) {
        var txn = transactionRepo.findByIdAndOrgAlias(transactionId, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + transactionId));

        if (!"pending".equals(txn.getStatus()) && !"completed".equals(txn.getStatus())) {
            throw new IllegalStateException("Cannot void transaction with status: " + txn.getStatus());
        }

        String previousStatus = txn.getStatus();
        txn.setStatus("voided");
        txn = transactionRepo.save(txn);

        // If it was completed, reverse the ledger entry
        if ("completed".equals(previousStatus)) {
            addLedgerEntry(txn.getPatientId(), "adjustment", txn.getAmount(),
                    "Voided payment", "payment_transaction", txn.getId());
        }

        return toTransactionDto(txn);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> transactionStats() {
        String org = orgAlias();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalTransactions", transactionRepo.countByOrgAlias(org));
        stats.put("pendingCount", transactionRepo.countByStatus(org, "pending"));
        stats.put("completedCount", transactionRepo.countByStatus(org, "completed"));
        stats.put("failedCount", transactionRepo.countByStatus(org, "failed"));
        stats.put("refundedCount", transactionRepo.countByStatus(org, "refunded"));
        stats.put("todayCollections", transactionRepo.sumCompletedSince(org, todayStart));
        stats.put("todayCount", transactionRepo.countCompletedSince(org, todayStart));
        stats.put("monthCollections", transactionRepo.sumCompletedSince(org, monthStart));
        stats.put("monthCount", transactionRepo.countCompletedSince(org, monthStart));
        return stats;
    }

    // ── Payment Plans ────────────────────────────────────────────────────

    @Transactional
    public PaymentPlanDto createPlan(PaymentPlanDto dto) {
        var plan = PaymentPlan.builder()
                .patientId(dto.getPatientId())
                .patientName(dto.getPatientName())
                .totalAmount(dto.getTotalAmount())
                .remainingAmount(dto.getTotalAmount())
                .installmentAmount(dto.getInstallmentAmount())
                .frequency(dto.getFrequency() != null ? dto.getFrequency() : "monthly")
                .paymentMethodId(dto.getPaymentMethodId())
                .autoCharge(dto.getAutoCharge() != null ? dto.getAutoCharge() : false)
                .nextPaymentDate(parseDate(dto.getNextPaymentDate()))
                .startDate(parseDate(dto.getStartDate()) != null ? parseDate(dto.getStartDate()) : LocalDate.now())
                .endDate(parseDate(dto.getEndDate()))
                .status("active")
                .installmentsTotal(dto.getInstallmentsTotal())
                .installmentsPaid(0)
                .referenceType(dto.getReferenceType())
                .referenceId(dto.getReferenceId())
                .notes(dto.getNotes())
                .orgAlias(orgAlias())
                .build();

        plan = planRepo.save(plan);
        return toPlanDto(plan);
    }

    @Transactional(readOnly = true)
    public PaymentPlanDto getPlan(Long id) {
        return planRepo.findByIdAndOrgAlias(id, orgAlias())
                .map(this::toPlanDto)
                .orElseThrow(() -> new NoSuchElementException("Payment plan not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PaymentPlanDto> listPlans(Long patientId) {
        return planRepo.findByOrgAliasAndPatientIdOrderByCreatedAtDesc(orgAlias(), patientId)
                .stream().map(this::toPlanDto).toList();
    }

    @Transactional
    public PaymentPlanDto updatePlan(Long id, PaymentPlanDto dto) {
        var plan = planRepo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Payment plan not found: " + id));

        if (dto.getInstallmentAmount() != null) plan.setInstallmentAmount(dto.getInstallmentAmount());
        if (dto.getFrequency() != null) plan.setFrequency(dto.getFrequency());
        if (dto.getPaymentMethodId() != null) plan.setPaymentMethodId(dto.getPaymentMethodId());
        if (dto.getAutoCharge() != null) plan.setAutoCharge(dto.getAutoCharge());
        if (dto.getNextPaymentDate() != null) plan.setNextPaymentDate(parseDate(dto.getNextPaymentDate()));
        if (dto.getEndDate() != null) plan.setEndDate(parseDate(dto.getEndDate()));
        if (dto.getStatus() != null) plan.setStatus(dto.getStatus());
        if (dto.getInstallmentsTotal() != null) plan.setInstallmentsTotal(dto.getInstallmentsTotal());
        if (dto.getInstallmentsPaid() != null) plan.setInstallmentsPaid(dto.getInstallmentsPaid());
        if (dto.getRemainingAmount() != null) plan.setRemainingAmount(dto.getRemainingAmount());
        if (dto.getNotes() != null) plan.setNotes(dto.getNotes());

        return toPlanDto(planRepo.save(plan));
    }

    @Transactional
    public PaymentPlanDto cancelPlan(Long id) {
        var plan = planRepo.findByIdAndOrgAlias(id, orgAlias())
                .orElseThrow(() -> new NoSuchElementException("Payment plan not found: " + id));
        plan.setStatus("cancelled");
        return toPlanDto(planRepo.save(plan));
    }

    // ── Ledger ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PatientLedgerDto> getLedger(Long patientId) {
        return ledgerRepo.findByOrgAliasAndPatientIdOrderByCreatedAtDesc(orgAlias(), patientId)
                .stream().map(this::toLedgerDto).toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long patientId) {
        return ledgerRepo.sumByPatient(orgAlias(), patientId);
    }

    @Transactional
    public PatientLedgerDto postCharge(Long patientId, BigDecimal amount, String description,
                                       String referenceType, Long referenceId) {
        return addLedgerEntry(patientId, "charge", amount, description, referenceType, referenceId);
    }

    @Transactional
    public PatientLedgerDto postPayment(Long patientId, BigDecimal amount, String description,
                                         Long transactionId) {
        // Payments are negative (reduce balance)
        return addLedgerEntry(patientId, "payment", amount.negate(), description,
                "payment_transaction", transactionId);
    }

    private PatientLedgerDto addLedgerEntry(Long patientId, String entryType, BigDecimal amount,
                                             String description, String referenceType, Long referenceId) {
        BigDecimal currentBalance = ledgerRepo.sumByPatient(orgAlias(), patientId);
        BigDecimal newBalance = currentBalance.add(amount);

        var entry = PatientLedger.builder()
                .patientId(patientId)
                .entryType(entryType)
                .amount(amount)
                .runningBalance(newBalance)
                .description(description)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .postedBy(RequestContext.get().getOrgName()) // placeholder: use actual user later
                .orgAlias(orgAlias())
                .build();

        entry = ledgerRepo.save(entry);
        return toLedgerDto(entry);
    }

    // ── Date Parsing ─────────────────────────────────────────────────────

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.contains("T")) return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDate();
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Failed to parse date '{}', returning null", s);
            return null;
        }
    }

    // ── DTO Mappers ──────────────────────────────────────────────────────

    private PaymentTransactionDto toTransactionDto(PaymentTransaction e) {
        return PaymentTransactionDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .paymentMethodId(e.getPaymentMethodId())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .status(e.getStatus())
                .transactionType(e.getTransactionType())
                .paymentMethodType(e.getPaymentMethodType())
                .cardBrand(e.getCardBrand())
                .lastFour(e.getLastFour())
                .description(e.getDescription())
                .referenceType(e.getReferenceType())
                .referenceId(e.getReferenceId())
                .invoiceNumber(e.getInvoiceNumber())
                .stripePaymentIntentId(e.getStripePaymentIntentId())
                .stripeChargeId(e.getStripeChargeId())
                .processorResponse(e.getProcessorResponse())
                .convenienceFee(e.getConvenienceFee())
                .refundAmount(e.getRefundAmount())
                .refundReason(e.getRefundReason())
                .receiptSent(e.getReceiptSent())
                .receiptEmail(e.getReceiptEmail())
                .collectedBy(e.getCollectedBy())
                .collectedAt(e.getCollectedAt() != null ? e.getCollectedAt().toString() : null)
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private PaymentPlanDto toPlanDto(PaymentPlan e) {
        return PaymentPlanDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .patientName(e.getPatientName())
                .totalAmount(e.getTotalAmount())
                .remainingAmount(e.getRemainingAmount())
                .installmentAmount(e.getInstallmentAmount())
                .frequency(e.getFrequency())
                .paymentMethodId(e.getPaymentMethodId())
                .autoCharge(e.getAutoCharge())
                .nextPaymentDate(e.getNextPaymentDate() != null ? e.getNextPaymentDate().toString() : null)
                .startDate(e.getStartDate() != null ? e.getStartDate().toString() : null)
                .endDate(e.getEndDate() != null ? e.getEndDate().toString() : null)
                .status(e.getStatus())
                .installmentsTotal(e.getInstallmentsTotal())
                .installmentsPaid(e.getInstallmentsPaid())
                .referenceType(e.getReferenceType())
                .referenceId(e.getReferenceId())
                .notes(e.getNotes())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .updatedAt(e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null)
                .build();
    }

    private PatientLedgerDto toLedgerDto(PatientLedger e) {
        return PatientLedgerDto.builder()
                .id(e.getId())
                .patientId(e.getPatientId())
                .entryType(e.getEntryType())
                .amount(e.getAmount())
                .runningBalance(e.getRunningBalance())
                .description(e.getDescription())
                .referenceType(e.getReferenceType())
                .referenceId(e.getReferenceId())
                .postedBy(e.getPostedBy())
                .invoiceNumber(e.getInvoiceNumber())
                .recipient(e.getRecipient())
                .issuer(e.getIssuer())
                .createdAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null)
                .build();
    }
}
