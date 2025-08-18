package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "medication_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "medication_name")
    private String medicationName;

    private String dosage;
    private String instructions;

    @Column(name = "date_issued")
    private String dateIssued;

    @Column(name = "prescribing_doctor")
    private String prescribingDoctor;

    private String status;

    @Column(name = "created_date", nullable = false)
    private String createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private String lastModifiedDate;
}