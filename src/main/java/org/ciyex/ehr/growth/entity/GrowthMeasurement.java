package org.ciyex.ehr.growth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "growth_measurement")
@Builder @NoArgsConstructor @AllArgsConstructor
public class GrowthMeasurement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long patientId;
    private String patientName;
    private LocalDate measurementDate;
    @Column(precision = 6, scale = 2)
    private BigDecimal ageMonths;
    private String gender;
    @Column(precision = 6, scale = 2)
    private BigDecimal weightKg;
    @Column(precision = 6, scale = 2)
    private BigDecimal heightCm;
    @Column(precision = 5, scale = 2)
    private BigDecimal bmi;
    @Column(precision = 5, scale = 2)
    private BigDecimal headCircCm;
    @Column(precision = 5, scale = 2)
    private BigDecimal weightPercentile;
    @Column(precision = 5, scale = 2)
    private BigDecimal heightPercentile;
    @Column(precision = 5, scale = 2)
    private BigDecimal bmiPercentile;
    @Column(precision = 5, scale = 2)
    private BigDecimal headCircPercentile;
    @Builder.Default
    private String chartStandard = "WHO";
    private Long encounterId;
    private String measuredBy;
    @Column(columnDefinition = "TEXT")
    private String notes;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
