package com.qiaben.ciyex.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientMedicalHistoryDto {

    private Long patientId;
    private String medicalCondition;
    private String diagnosisDetails;
    private LocalDateTime diagnosisDate;
    private String treatmentDetails;
    private Boolean isChronic;
}
