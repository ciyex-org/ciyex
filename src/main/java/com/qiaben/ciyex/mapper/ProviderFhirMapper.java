package com.qiaben.ciyex.mapper;


import com.qiaben.ciyex.dto.core.ProviderDTO;
import com.qiaben.ciyex.dto.fhir.FhirProviderReportDTO;

public class ProviderFhirMapper {

    /**
     * Converts ProviderDTO to FhirProviderReportDTO.
     *
     * @param providerDTO Provider DTO
     * @return FhirProviderReportDTO
     */
    public static FhirProviderReportDTO fromProviderDTO(ProviderDTO providerDTO) {
        FhirProviderReportDTO fhir = new FhirProviderReportDTO();
        // Map the relevant fields from ProviderDTO to FhirProviderReportDTO
        fhir.setResourceType("Practitioner");
        fhir.setName(providerDTO.getName());
        fhir.setPhone(providerDTO.getPhone());
        fhir.setEmail(providerDTO.getEmail());
        fhir.setAddress(providerDTO.getAddress());
        fhir.setSpecialization(providerDTO.getSpecialization());
        fhir.setLicenseNumber(providerDTO.getLicenseNumber());
        fhir.setType(providerDTO.getType());
        fhir.setDepartment(providerDTO.getDepartment());
        // Map any other fields as needed

        return fhir;
    }
}

