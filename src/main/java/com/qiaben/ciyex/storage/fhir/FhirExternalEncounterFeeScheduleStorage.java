package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.FeeScheduleDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalEncounterFeeScheduleStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalEncounterFeeScheduleStorage implements ExternalEncounterFeeScheduleStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(FeeScheduleDto dto) {
        log.info("FHIR EncounterFeeSchedule create org={}, patient={}, enc={}, name={}",
                dto.getOrgId(), dto.getPatientId(), dto.getEncounterId(), dto.getName());
        // Map to FHIR ChargeItemDefinition or Contract (encounter-scoped) as per your design
        return null;
    }

    @Override public void update(String externalId, FeeScheduleDto dto) { }
    @Override public Optional<FeeScheduleDto> get(String externalId) { return Optional.empty(); }
    @Override public void delete(String externalId) { }
    @Override public List<FeeScheduleDto> searchAll(Long orgId, Long patientId, Long encounterId, String q, String status) {
        return Collections.emptyList();
    }
}
