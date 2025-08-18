package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.HealthcareServiceDto;
import com.qiaben.ciyex.storage.ExternalHealthcareServiceStorage;
import org.hl7.fhir.r4.model.HealthcareService;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class FhirExternalHealthcareServiceStorage implements ExternalHealthcareServiceStorage {

    @Override
    public void saveHealthcareService(HealthcareServiceDto healthcareServiceDto) {
        // Create FHIR HealthcareService object
        HealthcareService fhirHealthcareService = new HealthcareService();

        // Mapping fields
        fhirHealthcareService.setName(healthcareServiceDto.getName());
       // fhirHealthcareService.setDescription(healthcareServiceDto.getDescription());

        CodeableConcept type = new CodeableConcept();
        type.setText(healthcareServiceDto.getType());
        fhirHealthcareService.setType(Collections.singletonList(type));

        // Send to external FHIR system
        // Example (pseudo-code): fhirClient.save(fhirHealthcareService);
    }

    @Override
    public HealthcareServiceDto getHealthcareServiceById(Long id) {
        // Fetch HealthcareService from external FHIR system
        // Example: HealthcareService fhirHealthcareService = fhirClient.getHealthcareService(id);

        HealthcareServiceDto healthcareServiceDto = new HealthcareServiceDto();
        healthcareServiceDto.setId(id);
        healthcareServiceDto.setName("Sample Healthcare Service");
        healthcareServiceDto.setDescription("This is a description");
        healthcareServiceDto.setType("General Healthcare");
        healthcareServiceDto.setLocation("Sample Location");

        return healthcareServiceDto;
    }
}
