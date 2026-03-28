package org.ciyex.ehr.immunization.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ImmunizationDto {
    private Long id;
    private Long patientId;
    private String patientName;
    private String vaccineName;
    private String cvxCode;
    private String lotNumber;
    private String manufacturer;
    private String administrationDate;
    private String expirationDate;
    private String site;
    private String route;
    private Integer doseNumber;
    private String doseSeries;
    private String administeredBy;
    private String orderingProvider;
    private String status;
    private String refusalReason;
    private String reaction;
    private String visDate;
    private String notes;
    private String createdAt;
    private String updatedAt;
}
