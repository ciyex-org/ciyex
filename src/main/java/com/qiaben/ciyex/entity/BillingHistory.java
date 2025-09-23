package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;
    private Long userId;

    /** Stripe PaymentIntent ID (pi_xxx) */
    private String stripePaymentIntentId;

    /** Stripe PaymentMethod ID (pm_xxx) */
    @Column(nullable = false)
    private String stripePaymentMethodId;

    private Double amount;

    /** SUCCESS, FAILED, PENDING, ARCHIVED, etc. */
    private String status;

    /** FK to InvoiceBill */
    @Column(name = "invoice_bill_id", insertable = false, updatable = false)
    private Long invoiceBillId;

    /** Relation to InvoiceBill entity */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_bill_id", referencedColumnName = "id")
    private InvoiceBill invoiceBill;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


