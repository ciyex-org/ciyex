package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ProviderNoteDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalProviderNoteStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalProviderNoteStorage implements ExternalProviderNoteStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(ProviderNoteDto dto) {
        log.info("FHIR ProviderNote create patientId={}, encounterId={}", dto.getPatientId(), dto.getEncounterId());
        // Map dto -> Composition (sections: Subjective/Objective/Assessment/Plan):contentReference[oaicite:8]{index=8}
        // return created Composition.id
        return null;
    }

    @Override
    public void update(String externalId, ProviderNoteDto dto) {
        log.info("FHIR ProviderNote update externalId={}", externalId);
        // Read Composition, update metadata + sections, PUT/PATCH:contentReference[oaicite:9]{index=9}
    }

    @Override
    public Optional<ProviderNoteDto> get(String externalId) {
        log.info("FHIR ProviderNote get externalId={}", externalId);
        // Read Composition -> map to DTO (narrative + sections):contentReference[oaicite:10]{index=10}
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) {
        log.info("FHIR ProviderNote delete externalId={}", externalId);
        // Delete Composition
    }

    @Override
    public List<ProviderNoteDto> searchAll(Long orgId, Long patientId) {
        // Query Composition by patient/type if desired
        return Collections.emptyList();
    }

    @Override
    public List<ProviderNoteDto> searchAll(Long orgId, Long patientId, Long encounterId) {
        // Query Composition by patient+encounter
        return Collections.emptyList();
    }
}
