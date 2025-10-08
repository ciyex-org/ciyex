package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String service;

    @Column(name = "billing_cycle", nullable = false)
    private String billingCycle; // "Yearly" or "Monthly"

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "status")
    private String status;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;
}
