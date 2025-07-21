package com.qiaben.ciyex.dto.fhir;



import lombok.Data;

@Data
public class FhirProviderReportDTO {
    private String resourceType = "Practitioner"; // FHIR type for provider
    private String name;
    private String phone;
    private String email;
    private String address;
    private String specialization;
    private String licenseNumber;
    private String type;
    private String department;

    // Add other relevant fields as needed
}
