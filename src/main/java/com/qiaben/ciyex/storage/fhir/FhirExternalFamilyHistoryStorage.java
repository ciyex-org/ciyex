package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.FamilyHistoryDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalFamilyHistoryStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalFamilyHistoryStorage implements ExternalFamilyHistoryStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(FamilyHistoryDto dto) {
        log.info("FHIR FH create patientId={}, encounterId={}", dto.getPatientId(), dto.getEncounterId());
        // var client = fhirClientProvider.getForCurrentOrg(); // or getForOrg(dto.getOrgId())
        // TODO: Map DTO -> FHIR FamilyMemberHistory resources (one per Entry) and return a bundle id or first id
        return null;
    }

    @Override
    public void update(String externalId, FamilyHistoryDto dto) {
        log.info("FHIR FH update externalId={}", externalId);
        // TODO
    }

    @Override
    public Optional<FamilyHistoryDto> get(String externalId) {
        log.info("FHIR FH get externalId={}", externalId);
        // TODO
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) {
        log.info("FHIR FH delete externalId={}", externalId);
        // TODO
    }

    @Override
    public List<FamilyHistoryDto> searchAll(Long orgId, Long patientId) {
        log.info("FHIR FH searchAll orgId={}, patientId={}", orgId, patientId);
        // TODO
        return Collections.emptyList();
    }

    @Override
    public List<FamilyHistoryDto> searchAll(Long orgId, Long patientId, Long encounterId) {
        log.info("FHIR FH searchAll orgId={}, patientId={}, encounterId={}", orgId, patientId, encounterId);
        // TODO
        return Collections.emptyList();
    }
}
