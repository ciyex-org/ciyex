package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "assigned_providers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class AssignedProvider extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "fhir_id", length = 255)
    private String fhirId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "role", length = 32, nullable = false)
    private String role;

    @Column(name = "start_date", length = 16)
    private String startDate; // keep as String per your schema

    @Column(name = "end_date", length = 16)
    private String endDate;   // keep as String per your schema

    @Column(name = "status", length = 24)
    private String status;

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

    // audit fields provided by AuditableEntity

    public LocalDateTime getCreatedAt() { return getCreatedDate(); }
    public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
    public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }
}
