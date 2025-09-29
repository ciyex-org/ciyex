package com.qiaben.ciyex.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeBillingHistoryDto {
    private Long id;
    private Long orgId;
    private Long userId;

    // Stripe identifiers
    private String stripePaymentIntentId;
    private String stripePaymentMethodId;

    private Double amount;
    private String status;

    // 🔑 Foreign key to InvoiceBill
    private Long invoiceBillId;

    // 📄 External info from InvoiceBill
    private String externalId;   // invoice external id/number
    private String invoiceUrl;   // hosted invoice link if available
    private String receiptUrl;   // Stripe receipt link if available

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
