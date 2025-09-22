package com.qiaben.ciyex.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingHistoryDto {
    private Long id;
    private Long orgId;
    private Long userId;

    private String stripePaymentIntentId;
    private String stripePaymentMethodId;

    private Double amount;
    private String status;

    // 🔑 New field to track foreign key reference
    private Long invoiceBillId;

    private String invoiceUrl;
    private String receiptUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
