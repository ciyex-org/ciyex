package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.DateTimeFinalizedDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalDateTimeFinalizedStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalDateTimeFinalizedStorage implements ExternalDateTimeFinalizedStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(DateTimeFinalizedDto dto) {
        log.info("FHIR Finalized create patient={} encounter={} target={}:{}",
                dto.getPatientId(), dto.getEncounterId(), dto.getTargetType(), dto.getTargetId());
        // Map to FHIR:
        // - Composition.attester/time & mode for documents
        // - Provenance with agent/target/occurred for general finalization
        return null;
    }

    @Override public void update(String externalId, DateTimeFinalizedDto dto) { }
    @Override public Optional<DateTimeFinalizedDto> get(String externalId) { return Optional.empty(); }
    @Override public void delete(String externalId) { }
    @Override public List<DateTimeFinalizedDto> searchAll(Long patientId) { return Collections.emptyList(); }
    @Override public List<DateTimeFinalizedDto> searchAll(Long patientId, Long encounterId) { return Collections.emptyList(); }
}
