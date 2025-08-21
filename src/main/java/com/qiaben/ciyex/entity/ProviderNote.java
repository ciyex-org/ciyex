package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "provider_note")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderNote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;          // FHIR Composition id (optional)

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "note_title")
    private String noteTitle;

    @Column(name = "note_type_code")
    private String noteTypeCode;

    @Column(name = "note_status")
    private String noteStatus;

    @Column(name = "author_practitioner_id")
    private String authorPractitionerId;

    @Column(name = "note_datetime")
    private String noteDateTime;

    @Column(name = "narrative", columnDefinition = "TEXT")
    private String narrative;

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
