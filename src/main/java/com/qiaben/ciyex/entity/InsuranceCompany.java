package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "insurance_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "payer_id")
    private String payerId;


    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    @Column(name = "fhir_id")  // To store FHIR record ID
    private String fhirId;

    @Column(name = "created_date", nullable = false)
    private String createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private String lastModifiedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InsuranceStatus status;  // <-- Added column
}
