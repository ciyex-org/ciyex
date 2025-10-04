package com.qiaben.ciyex.dto;



import java.math.BigDecimal;
import java.time.LocalDate;

public record PatientInvoiceLineDto(
        Long id,
        LocalDate dos,
        String code,
        String treatment,
        String provider,
        BigDecimal charge,
        BigDecimal allowed,
        BigDecimal insWriteOff,
        BigDecimal insPortion,
        BigDecimal patientPortion
) {}
