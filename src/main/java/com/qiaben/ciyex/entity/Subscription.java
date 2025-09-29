package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscriptions",schema = "practice_1")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orgid")   // ✅ match DB column
    private Long orgId;

    @Column(name = "userid")
    private Long userId;

    @Column(name = "service")
    private String service;

    @Column(name = "billingcycle")   // ✅ DB column is billingcycle
    private String billingCycle;

    @Column(name = "scope")
    private String scope;

    @Column(name = "status")
    private String status;

    @Column(name = "startdate")   // ✅ DB column is startdate
    private String startDate;

    @Column(name = "price")
    private Double price;
}
