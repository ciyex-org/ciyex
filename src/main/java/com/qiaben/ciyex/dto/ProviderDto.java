package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ProviderDto {
    // Core Identification (Master DB)
    private Long id; // System-generated unique identifier
    private String npi; // National Provider Identifier
    private Identification identification;
    private Long orgId; // Add orgId to DTO

    // Contact Information
    private Contact contact;

    // Professional Details
    private ProfessionalDetails professionalDetails;

    // Credentials & Affiliations
    private Credentials credentials;

    // System and Access
    private SystemAccess systemAccess;

    // Billing and Insurance
    private Billing billing;

    // Scheduling/Location
    private Scheduling scheduling;

    // Audit/Meta
    private Audit audit;

    // FHIR-specific ID
    private String fhirId; // External storage ID

    // Nested Classes

    @Data
    public static class Identification {
        private String firstName; // Legal first name
        private String lastName; // Legal last name
        private String middleName; // Optional
        private String prefix; // Optional
        private String suffix; // Optional
        private String gender; // Optional (FHIR/HL7 code)
        private String dateOfBirth; // Optional
        private String photo; // Optional (URL or file)
    }

    @Data
    public static class Contact {
        private String email; // Optional
        private String phoneNumber; // Optional
        private String mobileNumber; // Optional
        private String faxNumber; // Optional
        private Address address; // Optional (Street, city, state, postal, country)

        @Data
        public static class Address {
            private String street;
            private String city;
            private String state;
            private String postalCode;
            private String country;
        }
    }

    @Data
    public static class ProfessionalDetails {
        private String specialty; // Optional
        private String providerType; // Optional
        private String licenseNumber; // Yes (State medical license)
        private String licenseState; // Optional
        private String licenseExpiry; // Optional
        private String deaNumber; // Optional
        private String upin; // Optional
        private String taxonomyCode; // Optional
    }

    @Data
    public static class Credentials {
        private String educationDegrees; // Optional
        private String boardCertifications; // Optional
        private String affiliatedOrganizations; // Optional
        private String employmentStatus; // Optional
    }

    @Data
    public static class SystemAccess {
        private String username; // Yes (For EHR access)
        private String passwordHash; // Yes
        private String rolesPermissions; // Yes (EHR roles)
        private String status; // Optional
    }

    @Data
    public static class Billing {
        private String billingNpi; // Yes (NPI used for claims)
        private String taxId; // Optional
        private String medicareMedicaidId; // Optional
        private String credentialedPayers; // Optional
    }

    @Data
    public static class Scheduling {
        private String practiceLocations; // Optional
        private String availableHours; // Optional
        private String onCallStatus; // Optional
    }

    @Data
    public static class Audit {
        private String createdDate; // Yes
        private String lastModifiedDate; // Yes
        private String createdBy; // Optional
        private String updatedBy; // Optional
    }
}