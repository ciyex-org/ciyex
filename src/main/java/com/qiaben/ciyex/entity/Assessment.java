package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "assessment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId; // optional FHIR id

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "assessment_summary", columnDefinition = "TEXT")
    private String assessmentSummary;

    @Column(name = "plan_summary", columnDefinition = "TEXT")
    private String planSummary;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // JSON content for all checklists/sections; stays in this single table
    @Lob
    @Column(name = "sections_json", columnDefinition = "TEXT")
    private String sectionsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
