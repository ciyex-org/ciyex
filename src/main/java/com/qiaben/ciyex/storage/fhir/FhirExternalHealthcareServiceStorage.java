package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.storage.ExternalHealthcareServiceStorage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FhirExternalHealthcareServiceStorage implements ExternalHealthcareServiceStorage {

    @Override
    public HealthcareServiceDto createHealthcareService(HealthcareServiceDto dto, Long orgId) {
        // Logic to interact with FHIR server and create healthcare service
        // Returning the DTO after creating it in the external storage
        return dto;
    }

    @Override
    public List<HealthcareServiceDto> getHealthcareServicesByOrgId(Long orgId) {
        // Logic to fetch healthcare services from the FHIR server using orgId
        // Returning the list of HealthcareServiceDto from the external storage
        return List.of(new HealthcareServiceDto()); // Example return value
    }
}
