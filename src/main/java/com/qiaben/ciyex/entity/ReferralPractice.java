package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "referral_practices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralPractice {

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

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "fhir_id")
    private String fhirId;

    @Column(name = "created_date", nullable = false)
    private String createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private String lastModifiedDate;
}