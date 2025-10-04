package com.qiaben.ciyex.dto;



import java.math.BigDecimal;

public record PatientAccountCreditDto(
        Long patientId,
        BigDecimal balance
) {}

