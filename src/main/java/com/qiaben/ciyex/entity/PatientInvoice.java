package com.qiaben.ciyex.entity;


import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "patient_invoices")
public class PatientInvoice {


    public enum Status { OPEN, PARTIALLY_PAID, PAID, VOID }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column(nullable = true) private Long orgId;
    @Column(nullable = false) private Long patientId;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Status status = Status.OPEN;

    @Column(precision = 12, scale = 2) private BigDecimal insWO = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal ptBalance = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal insBalance = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal totalCharge = BigDecimal.ZERO;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PatientInvoiceLine> lines = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate void touch() { updatedAt = LocalDateTime.now(); }

private LocalDate backdate;

    public LocalDate getBackdate() {
        return backdate;
    }

    public void setBackdate(LocalDate backdate) {
        this.backdate = backdate;
    }
// getters/setters

    public void recalcTotals() {
        BigDecimal sum = lines.stream()
                .map(PatientInvoiceLine::getCharge)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.totalCharge = sum;

        BigDecimal ins = lines.stream()
                .map(l -> Optional.ofNullable(l.getInsPortion()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pt = lines.stream()
                .map(l -> Optional.ofNullable(l.getPatientPortion()).orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.insBalance = ins;
        this.ptBalance = pt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // public Long getOrgId() {
    //     return orgId;
    // }

    // public void setOrgId(Long orgId) {
    //     this.orgId = orgId;
    // }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public BigDecimal getInsWO() {
        return insWO;
    }

    public void setInsWO(BigDecimal insWO) {
        this.insWO = insWO;
    }

    public BigDecimal getPtBalance() {
        return ptBalance;
    }

    public void setPtBalance(BigDecimal ptBalance) {
        this.ptBalance = ptBalance;
    }

    public BigDecimal getInsBalance() {
        return insBalance;
    }

    public void setInsBalance(BigDecimal insBalance) {
        this.insBalance = insBalance;
    }

    public BigDecimal getTotalCharge() {
        return totalCharge;
    }

    public void setTotalCharge(BigDecimal totalCharge) {
        this.totalCharge = totalCharge;
    }

    public List<PatientInvoiceLine> getLines() {
        return lines;
    }

    public void setLines(List<PatientInvoiceLine> lines) {
        this.lines = lines;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
// ... standard getters/setters omitted for brevity
}
