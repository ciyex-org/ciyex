package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gps_billing_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsBillingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private Long userId;

    /** GPS Transaction ID */
    @Column(name = "gps_transaction_id", nullable = false, length = 64)
    private String gpsTransactionId;

    /** GPS Customer Vault ID used for payment */
    @Column(name = "gps_customer_vault_id", length = 64)
    private String gpsCustomerVaultId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /** GPS response status (success, failed, etc.) */
    @Column(nullable = false, length = 20)
    private String status;

    /** GPS response message */
    @Column(length = 500)
    private String responseMessage;

    /** Link to invoice record */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_bill_id")
    private InvoiceBill invoiceBill;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}