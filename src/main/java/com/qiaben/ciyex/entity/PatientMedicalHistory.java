//package com.qiaben.ciyex.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "patient_medical_history")
//@Getter @Setter
//@NoArgsConstructor @AllArgsConstructor @Builder
//public class PatientMedicalHistory {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "external_id")
//    private String externalId; // FHIR id (nullable)
//
//    
//    private Long orgId;
//
//    @Column(name = "patient_id", nullable = false)
//    private Long patientId;
//
//    @Column(name = "encounter_id", nullable = false)
//    private Long encounterId;
//
//    @Column(name = "description", columnDefinition = "TEXT")
//    private String description;
//
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
//}





package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "patient_medical_history")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class PatientMedicalHistory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "medical_condition", length = 255)
    private String medicalCondition;

    @Column(name = "condition_name", length = 255)
    private String conditionName;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "is_chronic")
    private Boolean isChronic;

    @Column(name = "diagnosis_date")
    private LocalDateTime diagnosisDate;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    // audit fields are provided by AuditableEntity

    @Column(name = "treatment_details", columnDefinition = "text")
    private String treatmentDetails;

    @Column(name = "diagnosis_details", columnDefinition = "text")
    private String diagnosisDetails;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    // --- eSign / Print ---
    @Column(name = "e_signed")
    @Builder.Default
    private Boolean eSigned = Boolean.FALSE;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "signed_by", length = 128)
    private String signedBy;

    @Column(name = "printed_at")
    private OffsetDateTime printedAt;

    // audit fields provided by AuditableEntity (createdDate/lastModifiedDate)
}
