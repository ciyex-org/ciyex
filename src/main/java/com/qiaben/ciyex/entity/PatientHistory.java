package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "patient_history")
@Getter
@Setter
public class PatientHistory extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    
    @Column(name = "history_data", columnDefinition = "TEXT")
    private String historyData;
    

    @Column(name = "external_id")
    private String externalId;
    
    @Column(name = "fhir_id")
    private String fhirId;
}