package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ProcedureDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalProcedureStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalProcedureStorage implements ExternalProcedureStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(ProcedureDto dto) {
        log.info("FHIR Procedure create patientId={}, encounterId={}, CPT={}", dto.getPatientId(), dto.getEncounterId(), dto.getCpt4());
        // var client = fhirClientProvider.getForCurrentTenant();
        // Build Procedure resource; set coding system to CPT if you maintain that system
        return null;
    }

    @Override public void update(String externalId, ProcedureDto dto) { log.info("FHIR Procedure update {}", externalId); }
    @Override public Optional<ProcedureDto> get(String externalId) { return Optional.empty(); }
    @Override public void delete(String externalId) { log.info("FHIR Procedure delete {}", externalId); }

    @Override public List<ProcedureDto> searchAll(Long orgId, Long patientId) { return Collections.emptyList(); }
    @Override public List<ProcedureDto> searchAll(Long orgId, Long patientId, Long encounterId) { return Collections.emptyList(); }
}
