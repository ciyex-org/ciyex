package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "immunizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Immunization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    

    private String cvxCode;
    private String dateTimeAdministered;
    private String amountAdministered;
    private String expirationDate;
    private String manufacturer;
    private String lotNumber;
    private String administratorName;
    private String administratorTitle;
    private String dateVisGiven;
    private String dateVisStatement;
    private String route;
    private String administrationSite;
    private String notes;
    private String informationSource;
    private String completionStatus;
    private String substanceRefusalReason;
    private String reasonCode;
    private String orderingProvider;

    private String createdDate;
    private String lastModifiedDate;
}
