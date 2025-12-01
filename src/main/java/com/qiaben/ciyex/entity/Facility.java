package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "facilities")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Facility extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    // Physical Address
    @Column(name = "physical_address", columnDefinition = "TEXT")
    private String physicalAddress;

    @Column(name = "physical_city")
    private String physicalCity;

    @Column(name = "physical_state")
    private String physicalState;

    @Column(name = "physical_zip_code")
    private String physicalZipCode;

    @Column(name = "physical_country")
    private String physicalCountry;

    // Mailing Address
    @Column(name = "mailing_address", columnDefinition = "TEXT")
    private String mailingAddress;

    @Column(name = "mailing_city")
    private String mailingCity;

    @Column(name = "mailing_state")
    private String mailingState;

    @Column(name = "mailing_zip_code")
    private String mailingZipCode;

    @Column(name = "mailing_country")
    private String mailingCountry;

    // Contact Information
    @Column(name = "phone")
    private String phone;

    @Column(name = "fax")
    private String fax;

    @Column(name = "website")
    private String website;

    @Column(name = "email")
    private String email;

    // Facility Details
    @Column(name = "color")
    private String color;

    @Column(name = "iban")
    private String iban;

    @Column(name = "pos_code")
    private String posCode;

    @Column(name = "facility_taxonomy")
    private String facilityTaxonomy;

    @Column(name = "clia_number")
    private String cliaNumber;

    @Column(name = "tax_id_type")
    private String taxIdType;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "billing_attn")
    private String billingAttn;

    @Column(name = "facility_lab_code")
    private String facilityLabCode;

    @Column(name = "npi")
    private String npi;

    @Column(name = "oid")
    private String oid;

    // Checkboxes
    @Column(name = "billing_location")
    private Boolean billingLocation;

    @Column(name = "accepts_assignment")
    private Boolean acceptsAssignment;

    @Column(name = "service_location")
    private Boolean serviceLocation;

    @Column(name = "primary_business_entity")
    private Boolean primaryBusinessEntity;

    @Column(name = "facility_inactive")
    private Boolean facilityInactive;

    // Additional Info
    @Column(name = "info", columnDefinition = "TEXT")
    private String info;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "fhir_id")
    private String fhirId;

    /**
     * Default values method (call this before saving).
     */
    public void applyDefaults() {
        if (isActive == null) {
            isActive = true;
        }
        if (billingLocation == null) {
            billingLocation = false;
        }
        if (acceptsAssignment == null) {
            acceptsAssignment = false;
        }
        if (serviceLocation == null) {
            serviceLocation = false;
        }
        if (primaryBusinessEntity == null) {
            primaryBusinessEntity = false;
        }
        if (facilityInactive == null) {
            facilityInactive = false;
        }
        if (color == null || color.isEmpty()) {
            color = "#3B82F6";
        }
        if (posCode == null || posCode.isEmpty()) {
            posCode = "01: Pharmacy **";
        }
        if (taxIdType == null || taxIdType.isEmpty()) {
            taxIdType = "EIN";
        }
    }
}
