package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "patient_courtesy_credit")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class InvoiceCourtesyCredit extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column( nullable = false)
    private Long invoiceId;

    @Column(nullable = false, length = 100)
    private String adjustmentType; // e.g., "Un-Collected", "Courtesy Adjustment", "Flat-rate", etc.

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Boolean isActive = true; // Track if this credit is currently active or has been removed

    @Column(nullable = false)
    private Boolean isDeleted = false;
}
