package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.PhysicalExamDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalPhysicalExamStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalPhysicalExamStorage implements ExternalPhysicalExamStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(PhysicalExamDto dto) {
        // var client = fhirClientProvider.getForCurrentOrg();
        // TODO: Map each SectionDto to FHIR (Observation or Composition section); return external id.
        return null;
    }

    @Override
    public void update(String externalId, PhysicalExamDto dto) {
        // TODO: read and update external resources
    }

    @Override
    public Optional<PhysicalExamDto> get(String externalId) {
        // TODO: read & map back to DTO
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) {
        // TODO: delete remote resources
    }

    @Override
    public List<PhysicalExamDto> searchAll(Long orgId, Long patientId) {
        // TODO: query FHIR for PE by patient
        return Collections.emptyList();
    }

    @Override
    public List<PhysicalExamDto> searchAll(Long orgId, Long patientId, Long encounterId) {
        // TODO: query FHIR by patient + encounter
        return Collections.emptyList();
    }
}
