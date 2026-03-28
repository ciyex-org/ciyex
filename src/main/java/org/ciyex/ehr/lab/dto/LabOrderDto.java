package org.ciyex.ehr.lab.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LabOrderDto {
    private Long id;
    private Long patientId;
    private String patientFirstName;
    private String patientLastName;
    private String patientHomePhone;
    private String mrn;
    private String orderNumber;
    private String orderName;
    private String testCode;
    private String testDisplay;
    private String status;
    private String priority;
    private String orderDate;
    private String orderDateTime;
    private String labName;
    private String orderingProvider;
    private String physicianName;
    private String specimenId;
    private String diagnosisCode;
    private String procedureCode;
    private String result; // maps to resultStatus
    private String notes;
    private String createdAt;
    private String updatedAt;
}
