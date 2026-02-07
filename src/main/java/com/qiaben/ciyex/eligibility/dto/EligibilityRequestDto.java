package com.qiaben.ciyex.eligibility.dto;



import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EligibilityRequestDto {
    @NotNull(message = "Patient ID is required")
    private Long patientId;
    
    private String serviceTypeCode; // Optional: 30=Health Benefit Plan Coverage (default)
}
