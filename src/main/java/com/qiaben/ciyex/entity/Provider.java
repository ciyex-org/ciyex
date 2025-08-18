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
    private Long orgId; // Added for multi-tenancy
    private String npi; // National Provider Identifier

    private String firstName; // Legal first name
    private String lastName; // Legal last name
    private String middleName; // Optional
    private String prefix; // Optional
    private String suffix; // Optional
    private String gender; // Optional (FHIR/HL7 code)
    private String dateOfBirth; // Optional
    private String photo; // Optional (URL or file)

    private String email; // Optional
    private String phoneNumber; // Optional
    private String mobileNumber; // Optional
    private String faxNumber; // Optional
    private String address; // Optional (Serialized as a string for simplicity)

    private String specialty; // Optional
    private String providerType; // Optional
    private String licenseNumber; // Yes (State medical license)
    private String licenseState; // Optional
    private String licenseExpiry; // Optional

    private String externalId;

    private String createdDate; // Yes
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