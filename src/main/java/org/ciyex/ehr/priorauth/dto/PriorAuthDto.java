package org.ciyex.ehr.priorauth.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriorAuthDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String providerName;
    private String insuranceName;
    private String insuranceId;
    private String memberId;
    private String authNumber;
    private String procedureCode;
    private String procedureDescription;
    private String diagnosisCode;
    private String diagnosisDescription;
    private String status;
    private String priority;
    private String requestedDate;
    private String reviewDate;
    private String approvedDate;
    private String deniedDate;
    private String expiryDate;
    private Integer approvedUnits;
    private Integer usedUnits;
    private Integer remainingUnits;
    private String denialReason;
    private String appealDeadline;
    private String notes;
    private String createdAt;
    private String updatedAt;
}
