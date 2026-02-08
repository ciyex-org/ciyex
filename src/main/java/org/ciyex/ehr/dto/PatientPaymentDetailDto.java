package org.ciyex.ehr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientPaymentDetailDto {
    
    private Long paymentId;
    private Long invoiceId;
    private String invoiceNumber;
    private LocalDateTime paymentDate;
    
    // Payment Information
    private String paymentMethod;
    private String chequeNumber;
    private String bankBranchNumber;
    
    // Financial Summary
    private BigDecimal patientAmount;
    private BigDecimal insuranceAmount;
    private BigDecimal previousTotalBalance;
    private BigDecimal paymentAmount;
    
    // Invoice level totals
    private BigDecimal ptPaid;
    private BigDecimal insPaid;
    
    // Line items detail
    private List<PatientPaymentLineDetailDto> lineDetails;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientPaymentLineDetailDto {
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
