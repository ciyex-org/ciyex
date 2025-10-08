package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_bills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "invoice_url")
    private String invoiceUrl;

    @Column(name = "receipt_url")
    private String receiptUrl;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // removed placeholder fields - orgId, userId, subscriptionId are mapped above

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
