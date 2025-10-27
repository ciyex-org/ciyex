





package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Entity
@Table(name = "enc_fee_schedule_entries",
        indexes = {
                @Index(name = "idx_efse_sched", columnList = "schedule_id"),
                @Index(name = "idx_efse_code", columnList = "code_type, code, modifier")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class EncounterFeeScheduleEntry extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "schedule_id", nullable = false)
    private EncounterFeeSchedule schedule;

    @Column(name = "code_type", length = 16, nullable = false)
    private String codeType;      // ICD9 | ICD10 | CPT4 | HCPCS | CUSTOM

    @Column(name = "code", length = 32, nullable = false)
    private String code;

    @Column(name = "modifier", length = 16)
    private String modifier;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "unit", length = 32)
    private String unit;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
      // ICD9 | ICD10 | CPT4 | HCPCS | CUSTOM

                // audit fields provided by AuditableEntity

                // Backwards-compatible accessors for existing code that expects createdAt/updatedAt
                public java.time.LocalDateTime getCreatedAt() { return getCreatedDate(); }
                public void setCreatedAt(java.time.LocalDateTime createdAt) { setCreatedDate(createdAt); }
                public java.time.LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
                public void setUpdatedAt(java.time.LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }

}
