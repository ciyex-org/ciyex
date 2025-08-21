package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.PlanDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalPlanStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalPlanStorage implements ExternalPlanStorage {
    private final FhirClientProvider fhirClientProvider;

    @Override public String create(PlanDto dto) { /* map to CarePlan/ServiceRequest */ return null; }
    @Override public void update(String externalId, PlanDto dto) { }
    @Override public Optional<PlanDto> get(String externalId) { return Optional.empty(); }
    @Override public void delete(String externalId) { }
    @Override public List<PlanDto> searchAll(Long orgId, Long patientId) { return Collections.emptyList(); }
    @Override public List<PlanDto> searchAll(Long orgId, Long patientId, Long encounterId) { return Collections.emptyList(); }
}
