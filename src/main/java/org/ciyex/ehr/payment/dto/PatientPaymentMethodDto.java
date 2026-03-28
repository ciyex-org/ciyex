package org.ciyex.ehr.payment.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientPaymentMethodDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String methodType;          // credit_card, debit_card, bank_account, fsa, hsa, check, cash, other
    private String cardBrand;           // visa, mastercard, amex, discover
    private String lastFour;
    private Integer expMonth;
    private Integer expYear;
    private String cardholderName;
    private String bankName;
    private String accountType;         // checking, savings
    private String routingLastFour;
    private String billingAddress;
    private String billingZip;
    private Boolean isDefault;
    private Boolean isActive;
    private String stripePaymentMethodId;
    private String stripeCustomerId;
    private String tokenReference;
    private String nickname;
    private String notes;
    private String createdAt;
    private String updatedAt;
}
