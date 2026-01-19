package com.qiaben.ciyex.dto;

import java.math.BigDecimal;
import java.util.List;

public record PatientInsuranceRemitWithLinesDto(
        Long id,
        Long patientId,
        Long invoiceId,
        Long insuranceId,
        BigDecimal submitted,
        BigDecimal balance,
        BigDecimal deductible,
        BigDecimal allowed,
        BigDecimal insWriteOff,
        BigDecimal insPay,
        String chequeNumber,
        String bankBranch,
        List<RemitLineDetail> lines
) {
    public record RemitLineDetail(
            Long invoiceLineId,
            BigDecimal submitted,
            BigDecimal allowed,
            BigDecimal insWriteOff,
            BigDecimal insPay,
            BigDecimal balance,
            BigDecimal deductible,
            Boolean updateAllowed,
            Boolean updateFlatPortion,
            Boolean applyWriteoff
    ) {}
}
