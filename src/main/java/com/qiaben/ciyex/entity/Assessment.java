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
//@Table(name = "assessment")
//@Getter @Setter
//@NoArgsConstructor @AllArgsConstructor @Builder
//public class Assessment {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "external_id")
//    private String externalId; // optional FHIR id
//
//    @Column(name = "org_id", nullable = false)
//    private Long orgId;
//
//    @Column(name = "patient_id", nullable = false)
//    private Long patientId;
//
//    @Column(name = "encounter_id", nullable = false)
//    private Long encounterId;
//
//    @Column(name = "assessment_summary", columnDefinition = "TEXT")
//    private String assessmentSummary;
//
//    @Column(name = "plan_summary", columnDefinition = "TEXT")
//    private String planSummary;
//
//    @Column(name = "notes", columnDefinition = "TEXT")
//    private String notes;
//
//    // JSON content for all checklists/sections; stays in this single table
//    @Lob
//    @Column(name = "sections_json", columnDefinition = "TEXT")
//    private String sectionsJson;
//
//    @CreationTimestamp
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
//
//
//}

package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "assessment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    // ---- Simple string fields matching your UI ----
    @Column(name = "diagnosis_code", length = 64)
    private String diagnosisCode;               // e.g. M54.5

    @Column(name = "diagnosis_name", length = 512)
    private String diagnosisName;               // e.g. Low back pain

    @Column(name = "status", length = 64)
    private String status;                      // e.g. Active

    @Column(name = "priority", length = 64)
    private String priority;                    // e.g. Primary

    @Column(name = "assessment_text", columnDefinition = "TEXT")
    private String assessmentText;              // Assessment / Impression

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ---- eSign / Print ----
    @Column(name = "e_signed")
    private Boolean eSigned = Boolean.FALSE;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "signed_by", length = 128)
    private String signedBy;

    @Column(name = "printed_at")
    private OffsetDateTime printedAt;

    // ---- audit ----
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
