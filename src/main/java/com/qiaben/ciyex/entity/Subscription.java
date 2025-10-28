package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Subscription extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    

    @Column(name = "user_id")
    private UUID userId;

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
