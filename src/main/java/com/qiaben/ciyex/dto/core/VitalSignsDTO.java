package com.qiaben.ciyex.dto.core;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSignsDTO {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Medical ID is required")
    private String medicalId;

    @NotNull(message = "Enter recorded body temperature")
    private Double bodyTemperature;

    @NotBlank(message = "Enter recorded heartbeat rate")
    private String heartRate;

    @NotNull(message = "Enter recorded systolic blood pressure")
    private Double systolic;

    @NotNull(message = "Enter recorded diastolic blood pressure")
    private Double diastolic;

    // Optional
    private Double respiratoryRate;

    // Optional
    private Double oxygenSaturation;

    @NotNull(message = "Enter recorded weight (Kg)")
    private Double weight;

    @NotNull(message = "Enter recorded height (Cm)")
    private Double height;
}
