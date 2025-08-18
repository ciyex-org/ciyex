package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.PractitionerRoleDto;
import com.qiaben.ciyex.storage.ExternalPractitionerRoleStorage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;

@Component
public class FhirExternalPractitionerRoleStorage implements ExternalPractitionerRoleStorage {

    @Override
    public PractitionerRoleDto createPractitionerRole(PractitionerRoleDto dto) {
        // Simulate external storage creation logic here (e.g., FHIR API call)
        return dto;  // Returning the same DTO for now as a placeholder
    }

    @Override
    public List<PractitionerRoleDto> getAllPractitionerRoles() {
        // Simulate fetching data from external storage
        return new ArrayList<>(); // Returning an empty list for now as a placeholder
    }
}
