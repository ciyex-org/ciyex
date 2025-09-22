package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payment_orders")
@Getter
@Setter
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stripePaymentIntentId;

    private Long amount; // in cents

    @Enumerated(EnumType.STRING)
    private PaymentOrderStatus status;
}
