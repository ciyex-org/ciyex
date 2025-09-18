package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_bills")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;
    private Long userId;
    private Long subscriptionId;

    private Double amount;

    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    private String invoiceUrl;
    private String receiptUrl;
    private String externalId;   // Added
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // Added
    private LocalDateTime dueDate;

}
