package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.entity.InvoiceStatus;   // ✅ Import the enum
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private InvoiceStatus status;   // ✅ Now resolved correctly

    private String invoiceUrl;
    private String receiptUrl;

    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private String externalId;
    private LocalDateTime updatedAt;

}
