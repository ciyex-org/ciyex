package com.qiaben.ciyex.dto;

import java.math.BigDecimal;

public record CourtesyCreditRequest(
    String adjustmentType, // e.g. "Un-Collected"
    BigDecimal amount,
    String description

) {}

