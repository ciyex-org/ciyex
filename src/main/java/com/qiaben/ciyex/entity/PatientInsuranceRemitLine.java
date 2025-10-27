package com.qiaben.ciyex.entity;



import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "patient_insurance_remit_lines")
public class PatientInsuranceRemitLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long orgId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Long getInvoiceLineId() {
        return invoiceLineId;
    }

    public void setInvoiceLineId(Long invoiceLineId) {
        this.invoiceLineId = invoiceLineId;
    }

    public BigDecimal getSubmitted() {
        return submitted;
    }

    public void setSubmitted(BigDecimal submitted) {
        this.submitted = submitted;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getDeductible() {
        return deductible;
    }

    public void setDeductible(BigDecimal deductible) {
        this.deductible = deductible;
    }

    public BigDecimal getAllowed() {
        return allowed;
    }

    public void setAllowed(BigDecimal allowed) {
        this.allowed = allowed;
    }

    public BigDecimal getInsWriteOff() {
        return insWriteOff;
    }

    public void setInsWriteOff(BigDecimal insWriteOff) {
        this.insWriteOff = insWriteOff;
    }

    public BigDecimal getInsPay() {
        return insPay;
    }

    public void setInsPay(BigDecimal insPay) {
        this.insPay = insPay;
    }

    public Boolean getUpdateAllowed() {
        return updateAllowed;
    }

    public void setUpdateAllowed(Boolean updateAllowed) {
        this.updateAllowed = updateAllowed;
    }

    public Boolean getUpdateFlatPortion() {
        return updateFlatPortion;
    }

    public void setUpdateFlatPortion(Boolean updateFlatPortion) {
        this.updateFlatPortion = updateFlatPortion;
    }

    public Boolean getApplyWriteoff() {
        return applyWriteoff;
    }

    public void setApplyWriteoff(Boolean applyWriteoff) {
        this.applyWriteoff = applyWriteoff;
    }

    @Column(nullable = false) private Long patientId;
    @Column(nullable = false) private Long invoiceId;
    @Column(nullable = false) private Long invoiceLineId;

    @Column(precision = 12, scale = 2) private BigDecimal submitted = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal balance = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal deductible = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal allowed = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal insWriteOff = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal insPay = BigDecimal.ZERO;

    private Boolean updateAllowed;
    private Boolean updateFlatPortion;
    private Boolean applyWriteoff;

    // getters/setters
}
