package com.qiaben.ciyex.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Immunization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Long orgId; // Organization ID for multi-tenancy
}
