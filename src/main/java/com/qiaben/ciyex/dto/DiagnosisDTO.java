package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisDTO {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Medical ID is required")
    private String medicalId;

    @NotBlank(message = "Doctor ID is required")
    private String doctorId;

    @NotBlank(message = "Symptoms required")
    private String symptoms;

    @NotBlank(message = "Diagnosis required")
    private String diagnosis;

    // Optional fields
    private String notes;

    private String prescribedMedications;

    private String followUpPlan;
}

