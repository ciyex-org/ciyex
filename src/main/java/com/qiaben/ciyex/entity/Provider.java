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
public class Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // System-generated unique identifier

    @Column(name = "org_id")
    private Long orgId; // Added for multi-tenancy

    private String npi; // National Provider Identifier

    @Column(name = "first_name")
    private String firstName; // Legal first name

    @Column(name = "last_name")
    private String lastName; // Legal last name

    @Column(name = "middle_name")
    private String middleName; // Optional

    private String prefix; // Optional
    private String suffix; // Optional
    private String gender; // Optional (FHIR/HL7 code)

    @Column(name = "date_of_birth")
    private String dateOfBirth; // Optional

    private String photo; // Optional (URL or file)
    private String email; // Optional

    @Column(name = "phone_number")
    private String phoneNumber; // Optional

    @Column(name = "mobile_number")
    private String mobileNumber; // Optional

    @Column(name = "fax_number")
    private String faxNumber; // Optional

    private String address; // Optional (Serialized as a string for simplicity)
    private String specialty; // Optional

    @Column(name = "provider_type")
    private String providerType; // Optional

    @Column(name = "license_number")
    private String licenseNumber; // Yes (State medical license)

    @Column(name = "license_state")
    private String licenseState; // Optional

    @Column(name = "license_expiry")
    private String licenseExpiry; // Optional

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "created_date")
    private String createdDate; // Yes

    @Column(name = "last_modified_date")
    private String lastModifiedDate; // Yes

    // New column
    @Enumerated(EnumType.STRING)
    private ProviderStatus status;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = ProviderStatus.ACTIVE; // default on create
        }
    } // Optional (Active, Inactive, etc.)
}
