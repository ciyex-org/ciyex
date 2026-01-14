package com.qiaben.ciyex.dto;

import java.math.BigDecimal;

public record PatientInsuranceRemitLineDto(
        Long id,
        Long patientId,
        Long invoiceId,
        Long insuranceId,
        Long invoiceLineId,
        BigDecimal submitted,
        BigDecimal balance,
        BigDecimal deductible,
        BigDecimal allowed,
        BigDecimal insWriteOff,
        BigDecimal insPay,
        Boolean updateAllowed,
        Boolean updateFlatPortion,
        Boolean applyWriteoff,
        String chequeNumber,
        String bankBranch
) {}
