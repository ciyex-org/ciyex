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

    @Column(name = "patient_external_id")
    private String patientExternalId;

    // EMR/UI fields
    @Column(name = "mrn")
    private String mrn;

    @Column(name = "encounter_id")
    private String encounterId;

    @Column(name = "physician_name")
    private String physicianName;

    @Column(name = "patient_first_name")
    private String patientFirstName;

    @Column(name = "patient_last_name")
    private String patientLastName;

    @Column(name = "patient_home_phone")
    private String patientHomePhone;

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

    // NEW
    @Column(name = "icd_id", length = 64)
    private String icdId;

    // Keep generous size; adjust to your DB’s text type if you expect large payloads/JSON.
    @Column(name = "result", length = 4096)
    private String result;

    @Column(name = "created_date")
    private String createdDate;

    @Column(name = "last_modified_date")
    private String lastModifiedDate;
}
