package com.qiaben.ciyex.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "orgs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Org {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "fhir_id")  // New field to store FHIR record ID
    private String fhirId;

    @Column(name = "org_name", nullable = false, unique = true)
    private String orgName;

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

    @Builder.Default
    @OneToMany(mappedBy = "org", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("org-userOrgRoles")
    private Set<UserOrgRole> userOrgRoles = new HashSet<>();

//    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference("org-encounters")
//    private Set<Encounter> encounters = new HashSet<>();
}
