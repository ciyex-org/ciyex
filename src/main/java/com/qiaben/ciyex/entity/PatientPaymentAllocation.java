package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "patient_payment_allocations")
public class PatientPaymentAllocation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id", nullable = false)


    private PatientPayment payment;

    @ManyToOne
    @JoinColumn(name = "invoice_line_id")
    private PatientInvoiceLine invoiceLine;

    @Column(nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    // audit fields provided by AuditableEntity

    // Constructors
    public PatientPaymentAllocation() {}

    public PatientPaymentAllocation(PatientPayment payment, PatientInvoiceLine invoiceLine, BigDecimal amount) {
        this.payment = payment;
        this.invoiceLine = invoiceLine;
        this.amount = amount;
        setCreatedDate(LocalDateTime.now());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientPayment getPayment() {
        return payment;
    }

    public void setPayment(PatientPayment payment) {
        this.payment = payment;
    }

    public PatientInvoiceLine getInvoiceLine() {
        return invoiceLine;
    }

    public void setInvoiceLine(PatientInvoiceLine invoiceLine) {
        this.invoiceLine = invoiceLine;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return getCreatedDate();
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        setCreatedDate(createdAt);
    }
}
