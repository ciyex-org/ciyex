package com.qiaben.ciyex.eligibility.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EligibilityTransaction {
    private String id;
    private String transactionId;
    private Long patientId;
    private String memberId;
    private String payerId;
    private String payerName;
    private String status;
    private String planName;
    private String coverageStartDate;
    private String coverageEndDate;
    private Double copayAmount;
    private Double deductibleAmount;
    private Double outOfPocketMax;
    private String x12Request;
    private String x12Response;
    private LocalDateTime requestTimestamp;
    private LocalDateTime responseTimestamp;
    private String errorMessage;
}
