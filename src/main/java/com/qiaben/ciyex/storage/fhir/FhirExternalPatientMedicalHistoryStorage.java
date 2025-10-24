package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalPatientMedicalHistoryStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalPatientMedicalHistoryStorage implements ExternalPatientMedicalHistoryStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(PatientMedicalHistoryDto dto) {
        // TODO: Map DTO -> FHIR (e.g., Observation/Condition/DocumentReference as per your model)
        // var client = fhirClientProvider.getForCurrentTenant(); // or getForOrg(orgId) if you changed provider
        log.info("FHIR PMH create for patientId={}, encounterId={}", dto.getPatientId(), dto.getEncounterId());
        // Return externalId from FHIR create outcome
        return null; // set real id
    }

    @Override
    public void update(String externalId, PatientMedicalHistoryDto dto) {
        log.info("FHIR PMH update externalId={}", externalId);
        // TODO: read + modify + update FHIR resource
    }

    @Override
    public Optional<PatientMedicalHistoryDto> get(String externalId) {
        log.info("FHIR PMH get externalId={}", externalId);
        // TODO: read FHIR -> map to DTO
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) {
        log.info("FHIR PMH delete externalId={}", externalId);
        // TODO: delete in FHIR
    }

    @Override
    public List<PatientMedicalHistoryDto> searchAll(Long orgId, Long patientId) {
        log.info("FHIR PMH searchAll orgId={}, patientId={}", orgId, patientId);
        // TODO: query FHIR by subject + org
        return Collections.emptyList();
    }

    @Override
    public List<PatientMedicalHistoryDto> searchAll(Long orgId, Long patientId, Long encounterId) {
        log.info("FHIR PMH searchAll orgId={}, patientId={}, encounterId={}", orgId, patientId, encounterId);
        // TODO: query FHIR by subject + encounter + org
        return Collections.emptyList();
    }
}
