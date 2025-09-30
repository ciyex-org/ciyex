package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
// Do not hard-code tenant schema here; the app controls search_path at runtime
@Table(name = "invoice_bills")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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
