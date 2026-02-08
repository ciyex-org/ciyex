package org.ciyex.ehr.dto;

public record InsurancePaymentResponseDto(
        Long insurancePaymentId,
        PatientInvoiceDto invoice
) {}
