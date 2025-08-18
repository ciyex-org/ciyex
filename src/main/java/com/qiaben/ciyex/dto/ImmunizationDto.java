package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ImmunizationDto {
    private Long id;
    private String vaccine;
    private String dose;
    private String dateAdministered;
    private String amountAdministered;
    private String immunizationExpirationDate;
    private String immunizationManufacturer;
    private String immunizationLotNumber;
    private String administratorName;
    private String dateInformationGiven;
    private String dateVISStatement;
    private String route;
    private String administrationSite;
    private String notes;
    private String informationSource;
    private String completionStatus;
    private String substanceRefusalReason;
    private String reasonCode;
    private String immunizationOrderingProvider;

    private Long patientId;
    private Long orgId; // Organization ID passed in header
}
