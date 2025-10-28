package com.qiaben.ciyex.dto;
import java.math.BigDecimal;

public record AccountAdjustmentRequest(
        String adjustmentType,        // "Flat-rate", "Total Outstanding", "Patient Outstanding", "Specific"
        BigDecimal flatRate,          // Used when adjustmentType is "Flat-rate"
        BigDecimal totalOutstanding,  // Used when adjustmentType is "Total Outstanding"
        BigDecimal patientOutstanding, // Used when adjustmentType is "Patient Outstanding"
        BigDecimal specificAmount,    // Used when adjustmentType is "Specific"
        Boolean includeCourtesyCredit, // Checkbox for including courtesy credit
        String description            // Optional description
) {}