package com.qiaben.ciyex.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsurancePaymentDetailDto {
    
    private Long remitId;
    private Long invoiceId;
    private String invoiceNumber;
    private LocalDate paymentDate;
    
    // Payment Information
    private String chequeNumber;
    private String bankBranchNumber;
    
    // Financial Summary
    private BigDecimal insWriteoff;
    private BigDecimal patientAmount;
    private BigDecimal insuranceAmount;
    private BigDecimal previousTotalBalance;
    private BigDecimal paymentAmount;
    
    // Invoice level totals
    private BigDecimal appliedWO;
    private BigDecimal ptPaid;
    private BigDecimal insPaid;
    
    // Line items detail
    private List<InsurancePaymentLineDetailDto> lineDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsurancePaymentLineDetailDto {
        private Long lineId;
        private String description;
        private String providerName;
        private BigDecimal amount;
        private BigDecimal patient;
        private BigDecimal insurance;
        private BigDecimal previousBalance;
        private BigDecimal payment;
    }
}
