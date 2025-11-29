package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "coverages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Coverage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")  // Added externalId
    private String externalId;

    @Column(name = "coverage_type")
    private String coverageType;

    @Column(name = "plan_name")
    private String planName;

    @Column(name = "policy_number")
    private String policyNumber;

    @Column(name = "coverage_start_date")
    private String coverageStartDate;

    @Column(name = "coverage_end_date")
    private String coverageEndDate;

    @Column(name = "patient_id")
    private Long patientId;

    

    @ManyToOne
    @JoinColumn(name = "insurance_company_id")
    private InsuranceCompany insuranceCompany;

    // Additional fields from the screenshot
    @Column(name = "provider")
    private String provider;

    @Column(name = "effective_date")
    private String effectiveDate;

    @Column(name = "effective_date_end")
    private String effectiveDateEnd;

    @Column(name = "group_number")
    private String groupNumber;

    @Column(name = "subscriber_employer")
    private String subscriberEmployer;

    @Column(name = "subscriber_address_line1")
    private String subscriberAddressLine1;

    @Column(name = "subscriber_address_line2")
    private String subscriberAddressLine2;

    @Column(name = "subscriber_city")
    private String subscriberCity;

    @Column(name = "subscriber_state")
    private String subscriberState;

    @Column(name = "subscriber_zip_code")
    private String subscriberZipCode;

    @Column(name = "subscriber_country")
    private String subscriberCountry;

    @Column(name = "subscriber_phone")
    private String subscriberPhone;

    // New fields for byholders, their relation, address, and copay
    @Column(name = "byholder_name")
    private String byholderName;

    @Column(name = "byholder_relation")
    private String byholderRelation;

    @Column(name = "byholder_address_line1")
    private String byholderAddressLine1;

    @Column(name = "byholder_address_line2")
    private String byholderAddressLine2;

    @Column(name = "byholder_city")
    private String byholderCity;

    @Column(name = "byholder_state")
    private String byholderState;

    @Column(name = "byholder_zip_code")
    private String byholderZipCode;

    @Column(name = "byholder_country")
    private String byholderCountry;

    @Column(name = "byholder_phone")
    private String byholderPhone;

    @Column(name = "copay_amount")
    private Double copayAmount;

    @Column(name = "card_front_url")
    private String cardFrontUrl;

    @Column(name = "card_back_url")
    private String cardBackUrl;
}
