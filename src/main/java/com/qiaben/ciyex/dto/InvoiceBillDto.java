package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.InvoiceStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceBillDto {
    private Long id;
    private Long orgId;
    private Long userId;
    private Long subscriptionId;

    private Double amount;
    private InvoiceStatus status;

    private String externalId;
    private String invoiceNumber;
    private String invoiceUrl;
    private String receiptUrl;

    private LocalDateTime dueDate;
    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
