package com.qiaben.ciyex.entity;



import jakarta.persistence.*;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name = "patient_account_credit", uniqueConstraints = {
        @UniqueConstraint(name = "uk_credit_patient", columnNames = {"orgId","patientId"})
})
@EqualsAndHashCode(callSuper = true)
public class PatientAccountCredit extends AuditableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Column(nullable = false) private Long orgId;
    @Column(nullable = false) private Long patientId;

    @Column(precision = 12, scale = 2) private BigDecimal balance = BigDecimal.ZERO;

    // getters/setters
}
