package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_billing_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeBillingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orgid")
    private Long orgId;

    @Column(name = "userid")
    private Long userId;

    @Column(name = "stripepaymentintentid")
    private String stripePaymentIntentId;

    @Column(name = "stripepaymentmethodid", nullable = false)
    private String stripePaymentMethodId;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "status")
    private String status;

    @Column(name = "invoice_bill_id")
    private Long invoiceBillId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "invoice_bill_id",
            referencedColumnName = "id",
            insertable = false,
            updatable = false
    )
    private InvoiceBill invoiceBill;

    @Column(name = "invoiceurl")
    private String invoiceUrl;

    @Column(name = "receipturl")
    private String receiptUrl;

    @Column(name = "createdat")
    private LocalDateTime createdAt;

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;
}
