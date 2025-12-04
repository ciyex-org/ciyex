package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "insurance_deposit")
@Data
@EqualsAndHashCode(callSuper = true)
public class InsuranceDeposit extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long policyId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable = false)
    private LocalDate depositDate;

    @Column(length = 500)
    private String paymentMethod;

    @Column(length = 500)
    private String providerId;

    @Column(length = 500)
    private String description;
}
