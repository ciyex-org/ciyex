package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.AssignedProviderDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalAssignedProviderStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalAssignedProviderStorage implements ExternalAssignedProviderStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(AssignedProviderDto dto) {
        log.info("FHIR AssignedProvider create patient={} encounter={} provider={}",
                dto.getPatientId(), dto.getEncounterId(), dto.getProviderId());
        // Map to Encounter.participant (type=attender/primary/consulting/referrer) or CareTeam.participant
        return null;
    }

    @Override public void update(String externalId, AssignedProviderDto dto) { }
    @Override public Optional<AssignedProviderDto> get(String externalId) { return Optional.empty(); }
    @Override public void delete(String externalId) { }
    @Override public List<AssignedProviderDto> searchAll(Long patientId) { return Collections.emptyList(); }
    @Override public List<AssignedProviderDto> searchAll(Long patientId, Long encounterId) { return Collections.emptyList(); }
}
