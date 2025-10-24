package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.PastMedicalHistoryDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalPastMedicalHistoryStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalPastMedicalHistoryStorage implements ExternalPastMedicalHistoryStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(PastMedicalHistoryDto dto) {
        log.info("FHIR PMH create patientId={}, encounterId={}", dto.getPatientId(), dto.getEncounterId());
        // var client = fhirClientProvider.getForCurrentTenant(); // or getForOrg(RequestContext.get().getTenantName())
        // TODO: Build & create FHIR resource; return externalId
        return null;
    }

    @Override
    public void update(String externalId, PastMedicalHistoryDto dto) {
        log.info("FHIR PMH update externalId={}", externalId);
        // TODO: read & update FHIR resource
    }

    @Override
    public Optional<PastMedicalHistoryDto> get(String externalId) {
        log.info("FHIR PMH get externalId={}", externalId);
        // TODO: map FHIR -> DTO
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) {
        log.info("FHIR PMH delete externalId={}", externalId);
        // TODO: delete in FHIR
    }

    @Override
    public List<PastMedicalHistoryDto> searchAll(Long orgId, Long patientId) {
        log.info("FHIR PMH searchAll orgId={}, patientId={}", orgId, patientId);
        // TODO: query FHIR
        return Collections.emptyList();
    }

    @Override
    public List<PastMedicalHistoryDto> searchAll(Long orgId, Long patientId, Long encounterId) {
        log.info("FHIR PMH searchAll orgId={}, patientId={}, encounterId={}", orgId, patientId, encounterId);
        // TODO: query FHIR
        return Collections.emptyList();
    }
}
