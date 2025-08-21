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


    // Extended Demographics Information (FHIR Extensions)
    private String preferredName;  // Preferred Name
    private String title;  // Title (e.g., Mr., Mrs., Dr.)
    private String birthName;  // Birth Name (if different from current name)
    private String licenseId;  // License/ID (for healthcare professionals)
    private String sexualOrientation;  // Sexual Orientation (e.g., Heterosexual, Homosexual, etc.)
    private String maritalStatus;  // Marital Status (e.g., Single, Married, Divorced)
    private String emergencyContact;  // Emergency Contact (e.g., name and phone number)
    private String race;  // Race (e.g., American Indian, Asian, etc.)
    private String ethnicity;  // Ethnicity (e.g., Hispanic, Non-Hispanic)
    private String nationality;  // Nationality (e.g., American, British, etc.)
    private String guardianName;  // Guardian Name (if applicable)
    private String guardianRelationship;  // Relationship to Guardian

    // Contact Information
    private String city;  // City (Optional)
    private String postalCode;  // Postal Code (Optional)
    private String country;  // Country (Optional)

    // Employer Information
    private String employerName;  // Employer Name (if applicable)
    private String employerAddress;  // Employer Address
    private String occupation;  // Occupation (e.g., Software Engineer, Doctor, etc.)

    // Choices and Miscellaneous Information
    private String provider;  // Provider Information (e.g., primary care provider)
    private String referringProvider;  // Referring Provider (if applicable)
    private String pharmacy;  // Pharmacy Information (if applicable)
    private String hipaaNoticeReceived;  // HIPAA Notice Acknowledgement (True/False)

    // Other Information
    private String language;  // Preferred language for communication
    private String billingNote;  // Any additional billing-related notes

    // Optional - For Record-Keeping
    private String previousNames;  // Any previous names the patient has used

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
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}