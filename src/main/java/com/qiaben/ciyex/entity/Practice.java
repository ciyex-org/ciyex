package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper=false)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "practice")
public class Practice extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    // Practice Settings
    @Column(name = "enable_patient_practice")
    private Boolean enablePatientPractice;

    // Regional Settings
    @Column(name = "units_for_visit_forms")
    private String unitsForVisitForms; // US, Metric, Both

    @Column(name = "display_format_us_weights")
    private String displayFormatUSWeights; // Show pounds as decimal value, Show pounds and ounces

    @Column(name = "telephone_country_code")
    private String telephoneCountryCode;

    @Column(name = "date_display_format")
    private String dateDisplayFormat; // YYYY-MM-DD, MM/DD/YYYY, DD/MM/YYYY

    @Column(name = "time_display_format")
    private String timeDisplayFormat; // 24 hr, 12 hr

    @Column(name = "time_zone")
    private String timeZone;

    @Column(name = "currency_designator")
    private String currencyDesignator;

    // Contact Information
    @Column(name = "email")
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "fax_number")
    private String faxNumber;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    // External Storage ID
    @Column(name = "external_id")
    private String externalId;

    @PrePersist
    public void prePersist() {
        if (enablePatientPractice == null) {
            enablePatientPractice = true;
        }
        if (unitsForVisitForms == null) {
            unitsForVisitForms = "Both";
        }
        if (displayFormatUSWeights == null) {
            displayFormatUSWeights = "Show pounds as decimal value";
        }
        if (telephoneCountryCode == null) {
            telephoneCountryCode = "1";
        }
        if (dateDisplayFormat == null) {
            dateDisplayFormat = "YYYY-MM-DD";
        }
        if (timeDisplayFormat == null) {
            timeDisplayFormat = "24 hr";
        }
        if (timeZone == null) {
            timeZone = "Unassigned";
        }
        if (currencyDesignator == null) {
            currencyDesignator = "$";
        }
    }
}