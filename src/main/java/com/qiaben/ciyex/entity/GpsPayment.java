package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gps_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class GpsPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "org_id")
    private Long orgId;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    /**
     * Internal numeric card reference (GPS only, optional).
     */
    @Column(name = "card_id")
    private Long cardId;

    /**
     * External string card reference (Stripe or other gateways).
     * Example: "card_1234"
     */
    @Column(name = "card_ref", length = 64)
    private String cardRef;

    /** GPS transaction ID returned from gateway */
    @Column(name = "gps_transaction_id", nullable = false, unique = true, length = 64)
    private String gpsTransactionId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency; // ISO 4217 (e.g., USD, INR, EUR)

    @Column(length = 20, nullable = false)
    private String status; // SUCCESS, FAILED, PENDING

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", updatable = false)
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
