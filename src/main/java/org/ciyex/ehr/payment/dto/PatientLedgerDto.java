package org.ciyex.ehr.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PatientLedgerDto {
    private Long id;
    private Long patientId;
    private String entryType;           // charge, payment, adjustment, refund, write_off, insurance_payment
    private BigDecimal amount;          // positive for charges, negative for payments/adjustments
    private BigDecimal runningBalance;
    private String description;
    private String referenceType;       // encounter, claim, payment_transaction, etc.
    private Long referenceId;
    private String postedBy;
    private String invoiceNumber;
    private String recipient;
    private String issuer;
    private String createdAt;
    // Immutable: no updatedAt
}
