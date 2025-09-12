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

    private String service;
    private String billingCycle; // Yearly | Monthly
    private String scope;        // Per Provider | Per Encounter
    private String status;       // Paid | Unpaid | Failed
    private String startDate;
    private Double price;
}
