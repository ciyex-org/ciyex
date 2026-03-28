package org.ciyex.ehr.prescription.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PrescriptionDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long encounterId;
    private String prescriberName;
    private String prescriberNpi;
    private String medicationName;
    private String medicationCode;
    private String medicationSystem;
    private String strength;
    private String dosageForm;
    private String sig;
    private Integer quantity;
    private String quantityUnit;
    private Integer daysSupply;
    private Integer refills;
    private Integer refillsRemaining;
    private String pharmacyName;
    private String pharmacyPhone;
    private String pharmacyAddress;
    private String status;
    private String priority;
    private String startDate;
    private String endDate;
    private String discontinuedDate;
    private String discontinuedReason;
    private String notes;
    private String deaSchedule;
    private String createdAt;
    private String updatedAt;
}
