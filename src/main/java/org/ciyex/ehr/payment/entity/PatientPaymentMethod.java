package org.ciyex.ehr.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "patient_payment_method")
@Builder @NoArgsConstructor @AllArgsConstructor
public class PatientPaymentMethod {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(columnDefinition = "TEXT")
    private String billingAddress;
    private String billingZip;
    private Boolean isDefault;
    private Boolean isActive;
    private String stripePaymentMethodId;
    private String stripeCustomerId;
    private String tokenReference;
    private String nickname;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
        if (isDefault == null) isDefault = false;
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
