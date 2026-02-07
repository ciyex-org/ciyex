package com.qiaben.ciyex.eligibility.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EligibilityRequestDto {
    @NotBlank
    private String patientFirstName;
    @NotBlank
    private String patientLastName;
    @NotBlank
    private String patientDob; // YYYYMMDD
    @NotBlank
    private String memberId;
    @NotBlank
    private String payerId;
    private String payerName;
    private String providerNpi;
    private String providerTaxId;
    private String serviceTypeCode; // 30=Health Benefit Plan Coverage
}
