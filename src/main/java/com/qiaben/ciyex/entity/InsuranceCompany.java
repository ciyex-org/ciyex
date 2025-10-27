package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insurance_companies")
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuranceCompany extends AuditableEntity {

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

    // audit fields provided by AuditableEntity

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InsuranceStatus status;  // <-- Added column
}
