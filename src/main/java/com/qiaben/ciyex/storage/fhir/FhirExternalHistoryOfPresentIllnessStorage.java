package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalHistoryOfPresentIllnessStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalHistoryOfPresentIllnessStorage implements ExternalHistoryOfPresentIllnessStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(HistoryOfPresentIllnessDto dto) {
        log.info("FHIR HPI create patientId={}, encounterId={}", dto.getPatientId(), dto.getEncounterId());
        // var client = fhirClientProvider.getForCurrentOrg(); // or getForOrg(dto.getOrgId()) if you prefer
        // TODO: build and create FHIR resource, return externalId
        return null;
    }

    @Override
    public void update(String externalId, HistoryOfPresentIllnessDto dto) {
        log.info("FHIR HPI update externalId={}", externalId);
        // TODO: read & update FHIR resource
    }

    @Override
    public Optional<HistoryOfPresentIllnessDto> get(String externalId) {
        log.info("FHIR HPI get externalId={}", externalId);
        // TODO: map FHIR -> DTO
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) {
        log.info("FHIR HPI delete externalId={}", externalId);
        // TODO: delete in FHIR
    }

    @Override
    public List<HistoryOfPresentIllnessDto> searchAll(Long orgId, Long patientId) {
        log.info("FHIR HPI searchAll orgId={}, patientId={}", orgId, patientId);
        // TODO: query FHIR
        return Collections.emptyList();
    }

    @Override
    public List<HistoryOfPresentIllnessDto> searchAll(Long orgId, Long patientId, Long encounterId) {
        log.info("FHIR HPI searchAll orgId={}, patientId={}, encounterId={}", orgId, patientId, encounterId);
        // TODO: query FHIR
        return Collections.emptyList();
    }
}
