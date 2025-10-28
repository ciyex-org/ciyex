package com.qiaben.ciyex.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PatientPatientPaymentAllocationDto(
//        Long invoiceLineId,
//        BigDecimal amount
        Long id,
        Long invoiceLineId,
        BigDecimal amount,
        String paymentMethod,
        LocalDateTime createdAt
) {}
