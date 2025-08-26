package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "referral_providers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralProvider {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String name;
    private String specialty;
    private String address;
    private String city;
    private String state;
    @Column(name = "postal_code") private String postalCode;
    private String country;
    @Column(name = "phone_number") private String phoneNumber;
    private String email;
    @Column(name = "fhir_id") private String fhirId;

    // LAZY is fine because we JOIN FETCH in repository queries
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practice_id", nullable = false)
    private ReferralPractice practice;

    @Column(name = "created_date", nullable = false) private String createdDate;
    @Column(name = "last_modified_date", nullable = false) private String lastModifiedDate;
}
