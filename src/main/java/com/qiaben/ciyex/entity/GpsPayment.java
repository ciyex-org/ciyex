package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gps_payments")  // ✅ explicit schema
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "org_id")
    private Long orgId;

    @Column(nullable = false, name = "user_id")
    private Long userId;

    /** Link to card used */
    @Column(nullable = false, name = "card_id")
    private Long cardId;

    /** GPS transaction ID returned from gateway */
    @Column(name = "gps_transaction_id", nullable = false, unique = true, length = 64)
    private String gpsTransactionId;

    private BigDecimal amount;
    private String currency;
    private String status; // e.g. SUCCESS, FAILED, PENDING
    private String description;

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
