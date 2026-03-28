package org.ciyex.ehr.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "patient_ledger")
@Builder @NoArgsConstructor @AllArgsConstructor
public class PatientLedger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private String orgAlias;
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
    // Immutable: no updatedAt, no @PreUpdate
}
