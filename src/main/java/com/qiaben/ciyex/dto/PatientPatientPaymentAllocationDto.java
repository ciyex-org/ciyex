package com.qiaben.ciyex.dto;



import java.math.BigDecimal;

public record PatientPatientPaymentAllocationDto(
        Long invoiceLineId,
        BigDecimal amount
) {}
