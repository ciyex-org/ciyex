package com.qiaben.ciyex.dto.fhir;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FhirVitalSignsDTO {

    private String patientId;
    private String medicalId;
    private Double bodyTemperature;
    private String heartRate;
    private Double systolic;
    private Double diastolic;
    private Double respiratoryRate;
    private Double oxygenSaturation;
    private Double weight;
    private Double height;
}
