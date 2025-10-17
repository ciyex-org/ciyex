package com.qiaben.ciyex.dto;

import java.math.BigDecimal;

public record InsuranceDepositRequest(
    Long policyId,
    Long providerId,
    BigDecimal amount,
    String paymentMethod,
    String chequeNumber,
    String bankBranch,
    String description

) {}
