package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lab_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // Patient linkage
    @Column(name = "patient_id")
    private Long patientId;

    
    // Removed patientExternalId & mrn (no longer stored in lab_orders table)
    @Column(name = "physician_name")
    private String physicianName;

    // Removed patientFirstName, patientLastName, patientHomePhone from schema

    @Column(name = "order_datetime")
    private String orderDateTime;

    @Column(name = "order_name")
    private String orderName;

    @Column(name = "lab_name")
    private String labName;

    // Technicals
    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "test_code")
    private String testCode;

    @Column(name = "test_display")
    private String testDisplay;

    private String status;
    private String priority;

    @Column(name = "order_date")
    private String orderDate;

    @Column(name = "specimen_id")
    private String specimenId;

    @Column(length = 2048)
    private String notes;

    @Column(name = "ordering_provider")
    private String orderingProvider;

    // UPDATED
    @Column(name = "diagnosis_code", length = 64)
    private String diagnosisCode;   // replaces icdId

    @Column(name = "procedure_code", length = 64)
    private String procedureCode;   // new

    @Column(name = "result", length = 4096)
    private String result;

    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "last_modified_date")
    private String lastModifiedDate;


    @Column(name = "fhir_external_id")
    private String externalId; // stores ServiceRequest id
}

