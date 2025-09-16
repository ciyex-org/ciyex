package com.qiaben.ciyex.dto;

import lombok.*;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalsDto {
    private Long id;
    private Long orgId;
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

    private Boolean signed;

    private LocalDateTime recordedAt;
    private LocalDateTime createdDate;
    private LocalDateTime lastModifiedDate;
}
