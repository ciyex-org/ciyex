package com.qiaben.ciyex.dto;

/**
 * Response DTO for patient payment operations.
 * Contains the payment ID and updated invoice data.
 */
public record PatientPaymentResponseDto(
        Long paymentId,
        PatientInvoiceDto invoice
) {}
