package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "patient_deposit")
@Data
@EqualsAndHashCode(callSuper = true)
public class PatientDeposit extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate depositDate;

    @Column(length = 500)
    private String description;

    @Column( length = 500)
    private String paymentMethod;
}

