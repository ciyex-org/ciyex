package com.qiaben.ciyex.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public  class StatementLineDto {
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private BigDecimal paid;
    private BigDecimal balance;
    private String type; // "charge", "payment", etc.
    // Additional fields for enhanced print layout
    private String providerName;
    private String procedureCode;
    private BigDecimal insurancePayment;
    private BigDecimal patientPayment;
    private BigDecimal adjustment;

    // Getters and setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPaid() { return paid; }
    public void setPaid(BigDecimal paid) { this.paid = paid; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getProcedureCode() { return procedureCode; }
    public void setProcedureCode(String procedureCode) { this.procedureCode = procedureCode; }
    public BigDecimal getInsurancePayment() { return insurancePayment; }
    public void setInsurancePayment(BigDecimal insurancePayment) { this.insurancePayment = insurancePayment; }
    public BigDecimal getPatientPayment() { return patientPayment; }
    public void setPatientPayment(BigDecimal patientPayment) { this.patientPayment = patientPayment; }
    public BigDecimal getAdjustment() { return adjustment; }
    public void setAdjustment(BigDecimal adjustment) { this.adjustment = adjustment; }
}