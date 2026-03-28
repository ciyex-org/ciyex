package org.ciyex.ehr.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "payment_transaction")
@Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String patientName;
    private Long paymentMethodId;
    private BigDecimal amount;
    private String currency;            // USD
    private String status;              // pending, processing, completed, failed, refunded, partial_refund, voided
    private String transactionType;     // payment, refund, adjustment, write_off
    private String paymentMethodType;   // credit_card, debit_card, bank_account, fsa, hsa, cash, check
    private String cardBrand;
    private String lastFour;
    private String description;
    private String referenceType;       // encounter, claim, invoice, copay, balance
    private Long referenceId;
    private String invoiceNumber;
    private String stripePaymentIntentId;
    private String stripeChargeId;
    @Column(columnDefinition = "JSONB")
    private String processorResponse;
    private BigDecimal convenienceFee;
    private BigDecimal refundAmount;
    @Column(columnDefinition = "TEXT")
    private String refundReason;
    private Boolean receiptSent;
    private String receiptEmail;
    private String collectedBy;
    private LocalDateTime collectedAt;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
        if (currency == null) currency = "USD";
        if (status == null) status = "pending";
        if (transactionType == null) transactionType = "payment";
        if (processorResponse == null) processorResponse = "{}";
        if (convenienceFee == null) convenienceFee = BigDecimal.ZERO;
        if (refundAmount == null) refundAmount = BigDecimal.ZERO;
        if (receiptSent == null) receiptSent = false;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
