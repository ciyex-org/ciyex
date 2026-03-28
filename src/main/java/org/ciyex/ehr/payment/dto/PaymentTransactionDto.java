package org.ciyex.ehr.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentTransactionDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long paymentMethodId;
    private BigDecimal amount;
    private String currency;
    private String status;              // pending, processing, completed, failed, refunded, partial_refund, voided
    private String transactionType;     // payment, refund, adjustment, write_off
    private String paymentMethodType;   // credit_card, debit_card, bank_account, fsa, hsa, cash, check
    private String cardBrand;
    private String lastFour;
    private String description;
    private String referenceType;       // encounter, claim, invoice, copay, balance
    private Long referenceId;
    private String invoiceNumber;
    private String stripePaymentIntentId;
    private String stripeChargeId;
    private String processorResponse;   // JSONB as String
    private BigDecimal convenienceFee;
    private BigDecimal refundAmount;
    private String refundReason;
    private Boolean receiptSent;
    private String receiptEmail;
    private String collectedBy;
    private String collectedAt;
    private String notes;
    private String createdAt;
    private String updatedAt;
}
