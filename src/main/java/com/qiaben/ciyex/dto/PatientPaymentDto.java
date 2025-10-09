package com.qiaben.ciyex.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a patient payment.
 */
public record PatientPaymentDto(
        Long id,
        Long patientId,
        String paymentMethod,
        BigDecimal amount,
        LocalDateTime createdAt
) {}
