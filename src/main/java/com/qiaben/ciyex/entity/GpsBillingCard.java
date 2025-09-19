package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "gps_billing_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsBillingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private Long userId;

    /** GPS Customer Vault ID */
    @Column(name = "gps_customer_vault_id", nullable = false, unique = true, length = 64)
    private String gpsCustomerVaultId;

    /** GPS Transaction ID from tokenization */
    @Column(name = "gps_transaction_id", length = 64)
    private String gpsTransactionId;

    /** Card details snapshot */
    private String brand;
    private String last4;
    private Integer expMonth;
    private Integer expYear;

    /** Track default card per user */
    @Builder.Default
    @Column(nullable = false)
    private Boolean isDefault = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Auto-set timestamps */
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