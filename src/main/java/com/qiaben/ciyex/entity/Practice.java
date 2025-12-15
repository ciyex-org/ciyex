package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = false)
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

    @Column(name = "enable_patient_practice")
    private Boolean enablePatientPractice;

    @Column(name = "units_for_visit_forms")
    private String unitsForVisitForms;

    @Column(name = "display_format_us_weights")
    private String displayFormatUSWeights;

    @Column(name = "telephone_country_code")
    private String telephoneCountryCode;

    @Column(name = "date_display_format")
    private String dateDisplayFormat;

    @Column(name = "time_display_format")
    private String timeDisplayFormat;

    @Column(name = "time_zone")
    private String timeZone;

    @Column(name = "currency_designator")
    private String currencyDesignator;

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

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "fhir_id")
    private String fhirId;

    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes;

    @Column(name = "token_expiry_minutes")
    private Integer tokenExpiryMinutes;

    /**
     * Manual method to initialize default values.
     * Call this method inside your service before saving.
     */
    public void applyDefaults() {
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
        if (sessionTimeoutMinutes == null) {
            sessionTimeoutMinutes = 5; // Default 5 minutes
        }
        if (tokenExpiryMinutes == null) {
            tokenExpiryMinutes = 5; // Default 5 minutes
        }
    }
}
