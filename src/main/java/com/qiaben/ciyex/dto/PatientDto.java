package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class PatientDto {
    private Long id; // Database ID
    private String externalId; // ID from external storage (e.g., FHIR Patient ID)
    private Long orgId; // Tenant identifier
    private String firstName;
    private String lastName;
    private String middleName;
    private String gender;
    private String dateOfBirth;
    private String phoneNumber;
    private String email;
    private String address;
    private String medicalRecordNumber;
    private Audit audit;

    // 🔹 Missing field — add this
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
