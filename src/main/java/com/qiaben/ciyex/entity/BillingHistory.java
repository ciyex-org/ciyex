package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Org and user */
    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Provider type: STRIPE or GPS */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private BillingProvider provider;

    /* ------------------- Stripe fields ------------------- */
    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @Column(name = "stripe_payment_method_id")
    private String stripePaymentMethodId;

    /* ------------------- GPS fields ------------------- */
    @Column(name = "gps_transaction_id", length = 64)
    private String gpsTransactionId;

    @Column(name = "gps_customer_vault_id", length = 64)
    private String gpsCustomerVaultId;

    /* ------------------- Common fields ------------------- */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingStatus status; // SUCCESS, FAILED, PENDING, etc.

    @Column(name = "response_message", length = 500)
    private String responseMessage;

    @Column(name = "response_code")
    private Integer responseCode;

    /** Link to invoice record */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_bill_id")
    private InvoiceBill invoiceBill;

    @Column(name = "invoice_url")
    private String invoiceUrl;

    @Column(name = "receipt_url")
    private String receiptUrl;

    /** Audit fields */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ------------------- Lifecycle hooks ------------------- */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /* ------------------- Enums ------------------- */
    public enum BillingProvider {
        STRIPE, GPS, INVOICE
    }

    public enum BillingStatus {
        SUCCEEDED,
        FAILED,
        PENDING,
        PROCESSING,
        CANCELED,
        ARCHIVED,
        DECLINED
    }
}
