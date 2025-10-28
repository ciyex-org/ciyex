package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Entity
@Table(name = "vitals")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vitals extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private Long encounterId;

    private Double weightKg;
    private Double weightLbs;
    private Double heightCm;
    private Double heightIn;
    private Double bpSystolic;
    private Double bpDiastolic;
    private Double pulse;
    private Double respiration;
    private Double temperatureC;
    private Double temperatureF;
    private Double oxygenSaturation;
    private Double bmi;
    private String notes;

    @Builder.Default
    private Boolean signed = false;

    private LocalDateTime recordedAt;
    // audit fields provided by AuditableEntity
}
