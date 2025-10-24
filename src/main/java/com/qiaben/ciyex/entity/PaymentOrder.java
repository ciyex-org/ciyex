package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Getter
@Setter
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stripe_payment_intent_id", length = 100, unique = true)
    private String stripePaymentIntentId;

    

    @Column(nullable = false)
    private Long amount; // total in cents

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentOrderStatus status;

    @Column(nullable = false, length = 20)
    private String method; // STRIPE, GPS, etc.

    @Column(name = "card_id", length = 100)
    private String cardId; // selected card reference

    @Lob
    @Column(name = "invoice_ids")
    private String invoiceIds; // comma-separated list of invoice IDs (legacy)

    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber; // user-facing invoice no. (e.g., INV-00001)

    @Column(name = "receipt_url")
    private String receiptUrl; // Stripe/GPS receipt link

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate; // ✅ new field for due date
}
