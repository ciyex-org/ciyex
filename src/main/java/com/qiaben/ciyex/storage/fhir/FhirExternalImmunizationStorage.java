package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.storage.ExternalImmunizationStorage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FhirExternalImmunizationStorage implements ExternalImmunizationStorage {

    @Override
    public ImmunizationDto createImmunization(ImmunizationDto dto, Long orgId) {
        // Logic to interact with FHIR server and create immunization
        // Returning the DTO after creating it in the external storage
        return dto;
    }

    @Override
    public List<ImmunizationDto> getImmunizationsByOrgId(Long orgId) {
        // Logic to fetch immunizations from the FHIR server using orgId
        // Returning the list of ImmunizationDto from the external storage
        return List.of(new ImmunizationDto()); // Example return value
    }
}
