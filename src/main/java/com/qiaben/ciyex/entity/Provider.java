package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "provider") // make sure it matches your table name
public class Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // System-generated unique identifier

    @Column(name = "org_id")
    private Long orgId; // Added for multi-tenancy


    @Column(name = "npi")
    private String npi; // National Provider Identifier

    @Column(name = "firstname")
    private String firstName; // Legal first name

    @Column(name = "lastname")
    private String lastName; // Legal last name

    @Column(name = "middlename")
    private String middleName; // Optional

    @Column(name = "prefix")

    private String prefix; // Optional

    @Column(name = "suffix")
    private String suffix; // Optional

    @Column(name = "gender")
    private String gender; // Optional (FHIR/HL7 code)


    @Column(name = "dateofbirth")
    private String dateOfBirth; // Optional

    @Column(name = "photo")
    private String photo; // Optional (URL or file)

    @Column(name = "email")
    private String email; // Optional

    @Column(name = "phonenumber")
    private String phoneNumber; // Optional

    @Column(name = "mobilenumber")
    private String mobileNumber; // Optional

    @Column(name = "faxnumber")
    private String faxNumber; // Optional

    @Column(name = "address")
    private String address; // Optional

    @Column(name = "specialty")
    private String specialty; // Optional

    @Column(name = "providertype")
    private String providerType; // Optional

    @Column(name = "licensenumber")
    private String licenseNumber; // Yes (State medical license)

    @Column(name = "licensestate")
    private String licenseState; // Optional

    @Column(name = "licenseexpiry")
    private String licenseExpiry; // Optional

    @Column(name = "externalid")
    private String externalId;

    @Column(name = "createddate")
    private String createdDate; // Yes

    @Column(name = "lastmodifieddate")
    private String lastModifiedDate; // Yes


  



    @Enumerated(EnumType.STRING)
    private ProviderStatus status;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = ProviderStatus.ACTIVE; // default on create
        }

