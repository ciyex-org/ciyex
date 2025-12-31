package com.qiaben.ciyex.dto;

import java.math.BigDecimal;
import java.util.List;

public record PatientInvoiceDto(
        Long id,
        Long patientId,
        PatientInvoiceStatus status,
        BigDecimal insWO,
        BigDecimal appliedWO,
        BigDecimal ptBalance,
        BigDecimal insBalance,
        BigDecimal totalCharge,

        List<PatientInvoiceLineDto> lines

) {}
