package org.ciyex.ehr.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "payment_plan")
@Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String patientName;
    private BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private BigDecimal installmentAmount;
    private String frequency;           // weekly, biweekly, monthly
    private Long paymentMethodId;
    private Boolean autoCharge;
    private LocalDate nextPaymentDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;              // active, completed, defaulted, cancelled, paused
    private Integer installmentsTotal;
    private Integer installmentsPaid;
    private String referenceType;
    private Long referenceId;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
        if (frequency == null) frequency = "monthly";
        if (status == null) status = "active";
        if (autoCharge == null) autoCharge = false;
        if (installmentsPaid == null) installmentsPaid = 0;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
