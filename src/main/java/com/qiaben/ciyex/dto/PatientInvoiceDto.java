package com.qiaben.ciyex.dto;



import com.qiaben.ciyex.entity.PatientInvoice.Status;
import java.math.BigDecimal;
import java.util.List;

public record PatientInvoiceDto(
        Long id,
        Long patientId,
        Status status,
        BigDecimal insWO,
        BigDecimal ptBalance,
        BigDecimal insBalance,
        BigDecimal totalCharge,

        List<PatientInvoiceLineDto> lines

) {}
