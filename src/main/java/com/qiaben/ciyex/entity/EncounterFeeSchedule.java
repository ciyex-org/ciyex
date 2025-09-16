


package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "enc_fee_schedules",
        indexes = {
                @Index(name = "idx_efs_scope", columnList = "org_id, patient_id, encounter_id"),
                @Index(name = "idx_efs_status", columnList = "org_id, status")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EncounterFeeSchedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
