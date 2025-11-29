package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatientDto {

    private Long id; // Database ID

    private String fhirId; // FHIR resource ID
    private String externalId; // Alias for fhirId (ID from external storage)

    // 🔹 Mandatory fields with validation annotations
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String middleName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotNull(message = "Date of birth is required")
    private String dateOfBirth;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;
    private String email;
    private String address;

    private String medicalRecordNumber;

    private Audit audit;

    // 🔹 Optional field - kept for internal record but not mandatory
    private String status;

    // Extended Demographics Information (FHIR Extensions)
    private String preferredName;
    private String title;
    private String birthName;

    private String licenseId;

    private String sexualOrientation;
    private String maritalStatus;

    private String emergencyContact;

    private String race;
    private String ethnicity;
    private String nationality;
    private String guardianName;
    private String guardianRelationship;

    // Contact Information
    private String city;
    private String postalCode;
    private String country;

    // Employer Information
    private String employerName;
    private String employerAddress;
    private String occupation;

    // Choices and Miscellaneous Information
    private String provider;
    private String referringProvider;
    private String pharmacy;
    private String hipaaNoticeReceived;

    // Other Information
    private String language;
    private String billingNote;

    // Optional - For Record-Keeping
    private String previousNames;

    // 🔹 Nested Classes

    @Data
    public static class Identification {
        private String firstName;
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
        private String email;
        private String phoneNumber;
        private String mobileNumber;
        private String faxNumber;
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
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}