package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "assessment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Assessment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(name = "fhir_id", length = 128)
    private String fhirId;

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
    @Builder.Default
    @Column(name = "e_signed")
    private Boolean eSigned = Boolean.FALSE;

    @Column(name = "signed_at")
    private OffsetDateTime signedAt;

    @Column(name = "signed_by", length = 128)
    private String signedBy;

    @Column(name = "printed_at")
    private OffsetDateTime printedAt;

    // audit fields are provided by AuditableEntity

    // Backwards-compatible accessors for existing code that expects createdAt/updatedAt
    public LocalDateTime getCreatedAt() { return getCreatedDate(); }
    public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
    public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }
}
