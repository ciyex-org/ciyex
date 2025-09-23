package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

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

    private Long orgId;     // 🔹 Track organization
    private Long userId;    // 🔹 Optional (who owns it)

    private String service;       // EHR, Telehealth, etc.
    private String billingCycle;  // Yearly | Monthly
    private String scope;         // Per Provider | Per Encounter
    private String status;        // Active | Cancelled
    private String startDate;     // Store as ISO string
    private Double price;
}
