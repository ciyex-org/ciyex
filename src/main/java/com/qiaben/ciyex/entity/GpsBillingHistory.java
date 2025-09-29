package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gps_billing_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsBillingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** GPS Transaction ID */
    @Column(name = "gps_transaction_id", nullable = false, length = 64, unique = true)
    private String gpsTransactionId;

    /** Customer Vault ID */
    @Column(name = "gps_customer_vault_id", length = 64)
    private String gpsCustomerVaultId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status;  // SUCCESS, FAILED, etc.

    /** Response message from GPS */
    @Column(name = "response_message", length = 500)
    private String responseMessage;

    /** Optional response code */
    @Column(name = "response_code")
    private Integer responseCode;

    /** Link to invoice record */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_bill_id")
    private InvoiceBill invoiceBill;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
