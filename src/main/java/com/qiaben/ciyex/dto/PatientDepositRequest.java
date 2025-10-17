package com.qiaben.ciyex.dto;

import java.math.BigDecimal;

public record PatientDepositRequest(BigDecimal amount, String description) {}

