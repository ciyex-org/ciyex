package org.ciyex.ehr.eligibility.dto;



import lombok.Data;
import java.util.List;

@Data
public class EligibilityResponseDto {
    private String transactionId;
    private String status; // Active, Inactive, Unknown
    private String planName;
    private String payerName;
    private String memberId;
    private String coverageStartDate;
    private String coverageEndDate;
    private Double copayAmount;
    private Double deductibleAmount;
    private Double deductibleRemaining;
    private Double outOfPocketMax;
    private Double outOfPocketRemaining;
    private List<ServiceCoverage> serviceCoverages;
    private String rawX12Response;
    
    @Data
    public static class ServiceCoverage {
        private String serviceType;
        private String serviceTypeCode;
        private String coverageLevel;
        private String timePeriod;
        private Double copay;
        private Double coinsurance;
    }
}
