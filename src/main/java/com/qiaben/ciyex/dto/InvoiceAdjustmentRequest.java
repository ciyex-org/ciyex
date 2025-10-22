package com.qiaben.ciyex.dto;
import java.math.BigDecimal;

public record InvoiceAdjustmentRequest(
        Long creditAdjustmentId,     // The adjustment record ID from UI
        String adjustmentType,       // e.g., "Un-Collected"
        Long invoiceId,              // Invoice being adjusted
        Integer percentageDiscount,  // Percentage discount (0-100)
        BigDecimal adjustmentAmount, // Added adjustment amount field
        String description           // Optional description
) {

}
