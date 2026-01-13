package com.qiaben.ciyex.dto;

public record InsurancePaymentResponseDto(
        Long insurancePaymentId,
        PatientInvoiceDto invoice
) {}
