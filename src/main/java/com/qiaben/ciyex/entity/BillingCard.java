package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orgId;

    @Column(nullable = false)
    private Long userId;

    /** Stripe PaymentMethod ID (pm_xxx) */
    @Column(name = "stripe_payment_method_id", nullable = false, unique = true, length = 64)
    private String stripePaymentMethodId;

    /** Stripe Customer ID (cus_xxx) */
    @Column(name = "stripe_customer_id", length = 64)
    private String stripeCustomerId;

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
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
