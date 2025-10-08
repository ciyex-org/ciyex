package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_billing_cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeBillingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "stripe_payment_method_id", length = 100)
    private String stripePaymentMethodId;

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(length = 50)
    private String brand;

    @Column(length = 10)
    private String last4;

    @Column(name = "exp_month")
    private Integer expMonth;

    @Column(name = "exp_year")
    private Integer expYear;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Convenience method: safely check if card is marked as default.
     */
    public boolean isDefaultCard() {
        return Boolean.TRUE.equals(isDefault);
    }
}
