package org.ciyex.ehr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TransferOfCreditDetailDto(
        Long id,
        LocalDate date,
        String description,
        BigDecimal amount,
        List<LineDetail> lines
) {
    public record LineDetail(
            String code,
            String treatment,
            BigDecimal insWriteoff,
            BigDecimal ptPortion,
            BigDecimal inPortion,
            BigDecimal totalCharge
    ) {}
}
