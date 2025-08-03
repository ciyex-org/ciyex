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