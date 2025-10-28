package com.qiaben.ciyex.dto.portal;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortalVitalsDto {
    private Long id;
    private Long patientId;
    private Long encounterId;

    // Basic vitals that patients can see
    private Double weightKg;
    private Double weightLbs;
    private Double bpSystolic;
    private Double bpDiastolic;
    private Double pulse;
    private Double respiration;
    private Double temperatureC;
    private Double temperatureF;
    private Double oxygenSaturation;

    // BMI calculation
    private Double bmi;

    // Notes (if any)
    private String notes;

    // When it was recorded
    private LocalDateTime recordedAt;
    private LocalDateTime createdDate;

    // Helper method to format blood pressure
    public String getFormattedBloodPressure() {
        if (bpSystolic != null && bpDiastolic != null) {
            return String.format("%.0f/%.0f", bpSystolic, bpDiastolic);
        }
        return null;
    }

    // Helper method to format temperature
    public String getFormattedTemperature() {
        if (temperatureF != null) {
            return String.format("%.1f °F", temperatureF);
        } else if (temperatureC != null) {
            return String.format("%.1f °C", temperatureC);
        }
        return null;
    }

    // Helper method to format weight
    public String getFormattedWeight() {
        if (weightLbs != null) {
            return String.format("%.1f lbs", weightLbs);
        } else if (weightKg != null) {
            return String.format("%.1f kg", weightKg);
        }
        return null;
    }

    // Helper method to format pulse
    public String getFormattedPulse() {
        return pulse != null ? String.format("%.0f bpm", pulse) : null;
    }

    // Helper method to format oxygen saturation
    public String getFormattedOxygenSaturation() {
        return oxygenSaturation != null ? String.format("%.1f%%", oxygenSaturation) : null;
    }
}