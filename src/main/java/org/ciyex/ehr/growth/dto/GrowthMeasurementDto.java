package org.ciyex.ehr.growth.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GrowthMeasurementDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String measurementDate;
    private BigDecimal ageMonths;
    private String gender;
    private BigDecimal weightKg;
    private BigDecimal heightCm;
    private BigDecimal bmi;
    private BigDecimal headCircCm;
    private BigDecimal weightPercentile;
    private BigDecimal heightPercentile;
    private BigDecimal bmiPercentile;
    private BigDecimal headCircPercentile;
    private String chartStandard;
    private Long encounterId;
    private String measuredBy;
    private String notes;
    private String createdAt;
    private String updatedAt;
}
