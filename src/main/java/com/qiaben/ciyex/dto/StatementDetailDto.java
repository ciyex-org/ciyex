package com.qiaben.ciyex.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class StatementDetailDto {
    private Long patientId;
    private String patientName;
    private String statementType; // "invoice" or "statement"
    private Long invoiceId; // nullable for statement
    private LocalDate statementDate;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal balanceDue;
    private List<StatementLineDto> lines;

    // Additional fields for enhanced print layout
    private String providerName;
    private String officeName;
    private String officeAddress;
    private String officePhone;
    private String officeEmail;
    private BigDecimal totalInsurancePayments;
    private BigDecimal totalPatientPayments;
    private BigDecimal totalAdjustments;
    private BigDecimal outstandingBalance;
    private BigDecimal estimatedRemainingInsurance;
    private BigDecimal estimatedRemainingInsuranceAdjustment;
    private BigDecimal accountCredit;
    private String nextScheduledAppointment;

    // Getters and setters
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getStatementType() { return statementType; }
    public void setStatementType(String statementType) { this.statementType = statementType; }
    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }
    public LocalDate getStatementDate() { return statementDate; }
    public void setStatementDate(LocalDate statementDate) { this.statementDate = statementDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
    public BigDecimal getBalanceDue() { return balanceDue; }
    public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }
    public List<StatementLineDto> getLines() { return lines; }
    public void setLines(List<StatementLineDto> lines) { this.lines = lines; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public String getOfficeName() { return officeName; }
    public void setOfficeName(String officeName) { this.officeName = officeName; }
    public String getOfficeAddress() { return officeAddress; }
    public void setOfficeAddress(String officeAddress) { this.officeAddress = officeAddress; }
    public String getOfficePhone() { return officePhone; }
    public void setOfficePhone(String officePhone) { this.officePhone = officePhone; }
    public String getOfficeEmail() { return officeEmail; }
    public void setOfficeEmail(String officeEmail) { this.officeEmail = officeEmail; }
    public BigDecimal getTotalInsurancePayments() { return totalInsurancePayments; }
    public void setTotalInsurancePayments(BigDecimal totalInsurancePayments) { this.totalInsurancePayments = totalInsurancePayments; }
    public BigDecimal getTotalPatientPayments() { return totalPatientPayments; }
    public void setTotalPatientPayments(BigDecimal totalPatientPayments) { this.totalPatientPayments = totalPatientPayments; }
    public BigDecimal getTotalAdjustments() { return totalAdjustments; }
    public void setTotalAdjustments(BigDecimal totalAdjustments) { this.totalAdjustments = totalAdjustments; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public BigDecimal getEstimatedRemainingInsurance() { return estimatedRemainingInsurance; }
    public void setEstimatedRemainingInsurance(BigDecimal estimatedRemainingInsurance) { this.estimatedRemainingInsurance = estimatedRemainingInsurance; }
    public BigDecimal getEstimatedRemainingInsuranceAdjustment() { return estimatedRemainingInsuranceAdjustment; }
    public void setEstimatedRemainingInsuranceAdjustment(BigDecimal estimatedRemainingInsuranceAdjustment) { this.estimatedRemainingInsuranceAdjustment = estimatedRemainingInsuranceAdjustment; }
    public BigDecimal getAccountCredit() { return accountCredit; }
    public void setAccountCredit(BigDecimal accountCredit) { this.accountCredit = accountCredit; }
    public String getNextScheduledAppointment() { return nextScheduledAppointment; }
    public void setNextScheduledAppointment(String nextScheduledAppointment) { this.nextScheduledAppointment = nextScheduledAppointment; }


}
