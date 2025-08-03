package com.qiaben.ciyex.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientFormDTO {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 30, message = "First name must be between 2 and 30 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 30, message = "Last name must be between 2 and 30 characters")
    private String lastName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth cannot be in the future")
    private java.util.Date dateOfBirth;

    @NotNull(message = "Height is required")
    @PositiveOrZero(message = "Height must be a positive number")
    private Double height;

    @NotNull(message = "Weight is required")
    @PositiveOrZero(message = "Weight must be a positive number")
    private Double weight;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip code is required")
    @Size(min = 5, message = "Zip code must be at least 5 characters")
    private String zipCode;

    @NotBlank(message = "Preferred contact method is required")
    @Pattern(regexp = "Email|Phone|Text", message = "Preferred contact method must be Email, Phone, or Text")
    private String preferredContactMethod;

    @NotBlank(message = "Preferred appointment type is required")
    @Pattern(regexp = "In-person|Visual|Both", message = "Preferred appointment type must be In-person, Visual, or Both")
    private String preferredAppointmentType;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "MALE|FEMALE|OTHER|PREFER_NOT_TO_SAY", message = "Gender must be MALE, FEMALE, OTHER, or PREFER_NOT_TO_SAY")
    private String gender;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be 10 digits")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
    private String address;

    @NotBlank(message = "Emergency contact name is required")
    @Size(min = 2, max = 50, message = "Emergency contact name must be between 2 and 50 characters")
    private String emergencyContactName;

    @NotBlank(message = "Emergency contact number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Emergency contact number must be 10 digits")
    private String emergencyContactNumber;

    @NotBlank(message = "Relation with emergency contact is required")
    @Pattern(regexp = "mother|father|husband|wife|other", message = "Relation must be one of: mother, father, husband, wife, other")
    private String relation;

    @NotBlank(message = "Marital status is required")
    @Pattern(regexp = "SINGLE|MARRIED|DIVORCED|WIDOWED", message = "Marital status must be SINGLE, MARRIED, DIVORCED, or WIDOWED")
    private String maritalStatus;

    // Optional fields
    private String bloodGroup;
    private String allergies;
    private String medicalConditions;
    private String medicalHistory;
    private String insuranceProvider;
    private String insuranceNumber;

    @AssertTrue(message = "You must agree to the privacy policy.")
    private boolean privacyConsent;

    @AssertTrue(message = "You must agree to the terms of service.")
    private boolean serviceConsent;

    @AssertTrue(message = "You must agree to the medical treatment terms.")
    private boolean medicalConsent;

    private String img;

    @NotBlank(message = "Year is required")
    @Pattern(regexp = "^\\d{4}$", message = "Year must be a 4-digit number")
    private String yearOfRegistration;
}
