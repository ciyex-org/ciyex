package org.ciyex.ehr.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentPlanDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private BigDecimal installmentAmount;
    private String frequency;           // weekly, biweekly, monthly
    private Long paymentMethodId;
    private Boolean autoCharge;
    private String nextPaymentDate;
    private String startDate;
    private String endDate;
    private String status;              // active, completed, defaulted, cancelled, paused
    private Integer installmentsTotal;
    private Integer installmentsPaid;
    private String referenceType;
    private Long referenceId;
    private String notes;
    private String createdAt;
    private String updatedAt;
}
