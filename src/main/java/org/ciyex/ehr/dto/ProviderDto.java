package org.ciyex.ehr.dto;

import org.ciyex.ehr.dto.ProviderStatus;
import lombok.Data;

@Data
public class ProviderDto {
    // Core Identification (Master DB)
    private Long id; // System-generated unique identifier
    
    @jakarta.validation.constraints.NotBlank(message = "NPI is required")
    private String npi; // National Provider Identifier
    
    @jakarta.validation.Valid
    private Identification identification; // Add orgId to DTO

    // Contact Information
    @jakarta.validation.Valid
    private Contact contact;

    // Professional Details
    @jakarta.validation.Valid
    private ProfessionalDetails professionalDetails;

    // Credentials & Affiliations
    @jakarta.validation.Valid
    private Credentials credentials;

    // System and Access
    @jakarta.validation.Valid
    private SystemAccess systemAccess;

    // Billing and Insurance
    @jakarta.validation.Valid
    private Billing billing;

    // Scheduling/Location
    @jakarta.validation.Valid
    private Scheduling scheduling;

    // Audit/Meta
    private Audit audit;

    // FHIR-specific ID
    private String fhirId; // External storage ID

    // Nested Classes

    @Data
    public static class Identification {
        @jakarta.validation.constraints.NotBlank(message = "First name is required")
        private String firstName;
        
        @jakarta.validation.constraints.NotBlank(message = "Last name is required")
        private String lastName;
        
        private String middleName;
        private String prefix;
        private String suffix;
        private String gender;
        private String dateOfBirth;
        private String photo;
    }

    @Data
    public static class Contact {
        @jakarta.validation.constraints.Email(message = "Email must be valid")
        private String email;
        private String phoneNumber;
        
        @jakarta.validation.constraints.NotBlank(message = "Mobile number is required")
        private String mobileNumber;
        private String faxNumber;
        
        @jakarta.validation.Valid
        private Address address;

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
        @jakarta.validation.constraints.NotBlank(message = "Specialty is required")
        private String specialty;
        
        @jakarta.validation.constraints.NotBlank(message = "Provider type is required")
        private String providerType;
        
        @jakarta.validation.constraints.NotBlank(message = "License number is required")
        private String licenseNumber;
        private String licenseState;
        private String licenseExpiry;
        private String deaNumber;
        private String upin;
        private String taxonomyCode;
    }

    @Data
    public static class Credentials {
        private String educationDegrees;
        private String boardCertifications;
        private String affiliatedOrganizations;
        private String employmentStatus;
    }

    @Data
    public static class SystemAccess {
        private String username;
        private String passwordHash;
        private String rolesPermissions;
        private ProviderStatus status;
    }

    @Data
    public static class Billing {
        private String billingNpi;
        private String taxId;
        private String medicareMedicaidId;
        private String credentialedPayers;
    }

    @Data
    public static class Scheduling {
        private String practiceLocations;
        private String availableHours;
        private String onCallStatus;
    }

    @Data
    public static class Audit {
        private String createdDate; // Yes
        private String lastModifiedDate; // Yes
    
    }
}