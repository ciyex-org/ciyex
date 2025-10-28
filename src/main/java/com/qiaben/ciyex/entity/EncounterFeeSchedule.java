package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "enc_fee_schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class EncounterFeeSchedule extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "payer", length = 128)
    private String payer;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "effective_from", length = 16)
    private String effectiveFrom;

    @Column(name = "effective_to", length = 16)
    private String effectiveTo;

    @Column(name = "status", length = 24)
    private String status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EncounterFeeScheduleEntry> entries = new ArrayList<>();

        // audit fields provided by AuditableEntity

        // Backwards-compatible accessors for existing code that expects createdAt/updatedAt
        public LocalDateTime getCreatedAt() { return getCreatedDate(); }
        public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
        public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
        public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }
}
