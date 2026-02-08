package org.ciyex.ehr.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditTransferDto(
        Long patientId,
        Long invoiceId,
        BigDecimal transferAmount,
        BigDecimal newCreditBalance,
        String transferType,
        LocalDateTime transferDate,
        String note
) {
    public static CreditTransferDto automatic(Long patientId, Long invoiceId, BigDecimal amount, BigDecimal newBalance) {
        return new CreditTransferDto(
                patientId,
                invoiceId,
                amount,
                newBalance,
                "AUTOMATIC",
                LocalDateTime.now(),
                "Automatic credit transfer after payment completion"
        );
    }
    
    public static CreditTransferDto manual(Long patientId, Long invoiceId, BigDecimal amount, BigDecimal newBalance, String note) {
        return new CreditTransferDto(
                patientId,
                invoiceId,
                amount,
                newBalance,
                "MANUAL",
                LocalDateTime.now(),
                note
        );
    }
}