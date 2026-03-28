package org.ciyex.ehr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreditAdjustmentDetailDto(
        Long id,
        LocalDate date,
        String description,
        BigDecimal amount,
        BigDecimal insWriteoff,
        BigDecimal patientBalance,
        BigDecimal insuranceBalance,
        BigDecimal previousTotalBalance,
        BigDecimal adjustmentAmount,
        List<LineDetail> lines
) {
    public record LineDetail(
            String code,
            String treatment,
            String provider,
            BigDecimal insWriteoff,
            BigDecimal ptPortion,
            BigDecimal inPortion,
            BigDecimal totalCharge,
            BigDecimal adjustment
    ) {}
}
