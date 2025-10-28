package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "patient_invoice_lines")
public class PatientInvoiceLine extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientInvoice getInvoice() {
        return invoice;
    }

    public void setInvoice(PatientInvoice invoice) {
        this.invoice = invoice;
    }

    public LocalDate getDos() {
        return dos;
    }

    public void setDos(LocalDate dos) {
        this.dos = dos;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public BigDecimal getCharge() {
        return charge;
    }

    public void setCharge(BigDecimal charge) {
        this.charge = charge;
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

    public BigDecimal getInsPortion() {
        return insPortion;
    }

    public void setInsPortion(BigDecimal insPortion) {
        this.insPortion = insPortion;
    }

    public BigDecimal getPatientPortion() {
        return patientPortion;
    }

    public void setPatientPortion(BigDecimal patientPortion) {
        this.patientPortion = patientPortion;
    }

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "invoice_id")
    private PatientInvoice invoice;

    private LocalDate dos;
    private String code;
    private String treatment;
    private String provider;

    @Column(precision = 12, scale = 2) private BigDecimal charge = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal allowed = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal insWriteOff = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal insPortion = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal patientPortion = BigDecimal.ZERO;

    // getters/setters
}
