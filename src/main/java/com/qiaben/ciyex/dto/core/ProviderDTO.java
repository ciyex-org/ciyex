package com.qiaben.ciyex.dto.core;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProviderDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be 10 digits")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
    private String address;

    @NotBlank(message = "Specialization is required")
    @Size(min = 2, message = "Specialization must be at least 2 characters")
    private String specialization;

    @NotBlank(message = "License number is required")
    @Size(min = 2, message = "License number must be at least 2 characters")
    private String licenseNumber;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "FULL|PART", message = "Type must be FULL or PART")
    private String type;

    @NotBlank(message = "Department is required")
    @Size(min = 2, message = "Department must be at least 2 characters")
    private String department;

    // Optional
    private String img;

    // Password is optional or can be empty string
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zip is required")
    private String zip;

    @NotBlank(message = "NPI number is required")
    private String npiNumber;

    @NotBlank(message = "Years in practice is required")
    private String yearsInPractice;
}

