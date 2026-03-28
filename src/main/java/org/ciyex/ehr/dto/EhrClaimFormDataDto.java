package org.ciyex.ehr.dto;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record EhrClaimFormDataDto(
    // Claim Info
    Long claimId,
    String claimNumber,
    String claimStatus,
    LocalDate claimDate,
    
    // Insurance/Payer Info
    InsuranceInfo insuranceInfo,
    
    // Policyholder/Subscriber Info
    PolicyholderInfo policyholderInfo,
    
    // Patient Info
    PatientInfo patientInfo,
    
    // Provider/Facility Info
    ProviderInfo providerInfo,
    
    // Service Records
    List<ServiceRecord> serviceRecords,
    
    // Financial Summary
    FinancialSummary financialSummary
) {
    
    @Builder
    public record InsuranceInfo(
        String companyName,
        String planName,
        String payerId,
        String policyNumber,
        String groupNumber,
        String address,
        String city,
        String state,
        String zipCode,
        String phone
    ) {}
    
    @Builder
    public record PolicyholderInfo(
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String subscriberId,
        String address,
        String city,
        String state,
        String zipCode,
        String phone,
        String relationshipToPatient
    ) {}
    
    @Builder
    public record PatientInfo(
        Long patientId,
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        String gender,
        String patientAccountNumber,
        String address,
        String city,
        String state,
        String zipCode,
        String phone,
        String email
    ) {}
    
    @Builder
    public record ProviderInfo(
        String providerName,
        String npi,
        String licenseNumber,
        String taxId,
        String facilityName,
        String address,
        String city,
        String state,
        String zipCode,
        String phone,
        String fax,
        String email
    ) {}
    
    @Builder
    public record ServiceRecord(
        Long lineId,
        LocalDate serviceDate,
        String procedureCode,
        String description,
        String providerName,
        BigDecimal chargeAmount,
        Integer units,
        String placeOfService,
        String diagnosisCode
    ) {}
    
    @Builder
    public record FinancialSummary(
        BigDecimal totalCharges,
        BigDecimal totalSubmitted,
        BigDecimal insuranceBalance,
        BigDecimal patientBalance
    ) {}
}
