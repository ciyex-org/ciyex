package com.qiaben.ciyex.dto;

import com.qiaben.ciyex.dto.ProviderStatus;
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
        private String firstName; // Legal first name
        
        @jakarta.validation.constraints.NotBlank(message = "Last name is required")
        private String lastName; // Legal last name
        
        @jakarta.validation.constraints.NotBlank(message = "Middle name is required")
        private String middleName;
        
        @jakarta.validation.constraints.NotBlank(message = "Prefix is required")
        private String prefix;
        
        @jakarta.validation.constraints.NotBlank(message = "Suffix is required")
        private String suffix;
        
        private String gender; // Optional (FHIR/HL7 code)
        private String dateOfBirth; // Optional
        
        @jakarta.validation.constraints.NotBlank(message = "Photo is required")
        private String photo;
    }

    @Data
    public static class Contact {
        @jakarta.validation.constraints.NotBlank(message = "Email is required")
        @jakarta.validation.constraints.Email(message = "Email must be valid")
        private String email;
        
        @jakarta.validation.constraints.NotBlank(message = "Phone number is required")
        private String phoneNumber;
        
        @jakarta.validation.constraints.NotBlank(message = "Mobile number is required")
        private String mobileNumber;
        
        @jakarta.validation.constraints.NotBlank(message = "Fax number is required")
        private String faxNumber;
        
        @jakarta.validation.Valid
        private Address address;

        @Data
        public static class Address {
            @jakarta.validation.constraints.NotBlank(message = "Street is required")
            private String street;
            
            @jakarta.validation.constraints.NotBlank(message = "City is required")
            private String city;
            
            @jakarta.validation.constraints.NotBlank(message = "State is required")
            private String state;
            
            @jakarta.validation.constraints.NotBlank(message = "Postal code is required")
            private String postalCode;
            
            @jakarta.validation.constraints.NotBlank(message = "Country is required")
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
        
        @jakarta.validation.constraints.NotBlank(message = "License state is required")
        private String licenseState;
        
        @jakarta.validation.constraints.NotBlank(message = "License expiry is required")
        private String licenseExpiry;
        
        @jakarta.validation.constraints.NotBlank(message = "DEA number is required")
        private String deaNumber;
        
        @jakarta.validation.constraints.NotBlank(message = "UPIN is required")
        private String upin;
        
        @jakarta.validation.constraints.NotBlank(message = "Taxonomy code is required")
        private String taxonomyCode;
    }

    @Data
    public static class Credentials {
        @jakarta.validation.constraints.NotBlank(message = "Education degrees is required")
        private String educationDegrees;
        
        @jakarta.validation.constraints.NotBlank(message = "Board certifications is required")
        private String boardCertifications;
        
        @jakarta.validation.constraints.NotBlank(message = "Affiliated organizations is required")
        private String affiliatedOrganizations;
        
        @jakarta.validation.constraints.NotBlank(message = "Employment status is required")
        private String employmentStatus;
    }

    @Data
    public static class SystemAccess {
        @jakarta.validation.constraints.NotBlank(message = "Username is required")
        private String username;
        
        @jakarta.validation.constraints.NotBlank(message = "Password hash is required")
        private String passwordHash;
        
        @jakarta.validation.constraints.NotBlank(message = "Roles permissions is required")
        private String rolesPermissions;
        
        private ProviderStatus status;
    }

    @Data
    public static class Billing {
        @jakarta.validation.constraints.NotBlank(message = "Billing NPI is required")
        private String billingNpi;
        
        @jakarta.validation.constraints.NotBlank(message = "Tax ID is required")
        private String taxId;
        
        @jakarta.validation.constraints.NotBlank(message = "Medicare/Medicaid ID is required")
        private String medicareMedicaidId;
        
        @jakarta.validation.constraints.NotBlank(message = "Credentialed payers is required")
        private String credentialedPayers;
    }

    @Data
    public static class Scheduling {
        @jakarta.validation.constraints.NotBlank(message = "Practice locations is required")
        private String practiceLocations;
        
        @jakarta.validation.constraints.NotBlank(message = "Available hours is required")
        private String availableHours;
        
        @jakarta.validation.constraints.NotBlank(message = "On call status is required")
        private String onCallStatus;
    }

    @Data
    public static class Audit {
        private String createdDate; // Yes
        private String lastModifiedDate; // Yes
    
    }
}