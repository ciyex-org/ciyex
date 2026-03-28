package org.ciyex.ehr.dto;

import java.math.BigDecimal;

public record CourtesyCreditRequest(
    String adjustmentType, // e.g. "Un-Collected", "Courtesy Adjustment"
    BigDecimal amount,
    String description,
    Boolean applyToInvoice, // If true, apply directly to a specific invoice
    Boolean closeInvoice // If true, close invoice after applying credit
) {}



