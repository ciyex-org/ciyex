
package com.qiaben.ciyex.dto;

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


    private Double respiratoryRate;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getMedicalId() {
        return medicalId;
    }

    public void setMedicalId(String medicalId) {
        this.medicalId = medicalId;
    }

    public Double getBodyTemperature() {
        return bodyTemperature;
    }

    public void setBodyTemperature(Double bodyTemperature) {
        this.bodyTemperature = bodyTemperature;
    }

    public String getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(String heartRate) {
        this.heartRate = heartRate;
    }

    public Double getSystolic() {
        return systolic;
    }

    public void setSystolic(Double systolic) {
        this.systolic = systolic;
    }

    public Double getDiastolic() {
        return diastolic;
    }

    public void setDiastolic(Double diastolic) {
        this.diastolic = diastolic;
    }

    public Double getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(Double respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public Double getOxygenSaturation() {
        return oxygenSaturation;
    }

    public void setOxygenSaturation(Double oxygenSaturation) {
        this.oxygenSaturation = oxygenSaturation;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    private Double oxygenSaturation;

    @NotNull(message = "Enter recorded weight (Kg)")
    private Double weight;

    @NotNull(message = "Enter recorded height (Cm)")
    private Double height;
}
