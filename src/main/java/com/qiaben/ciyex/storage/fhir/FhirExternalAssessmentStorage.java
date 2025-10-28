package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.AssessmentDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalAssessmentStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalAssessmentStorage implements ExternalAssessmentStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(AssessmentDto dto) {
        log.info("FHIR ASSESSMENT create patientId={}, encounterId={}", dto.getPatientId(), dto.getEncounterId());
        // var client = fhirClientProvider.getForCurrentTenant();
        // TODO: Map dto -> FHIR resources and return a bundle/primary id
        return null;
    }

    @Override
    public void update(String externalId, AssessmentDto dto) {
        log.info("FHIR ASSESSMENT update externalId={}", externalId);
        // TODO: read+update on FHIR side
    }

    @Override
    public Optional<AssessmentDto> get(String externalId) {
        log.info("FHIR ASSESSMENT get externalId={}", externalId);
        // TODO: map FHIR -> DTO
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) {
        log.info("FHIR ASSESSMENT delete externalId={}", externalId);
        // TODO: delete in FHIR
    }

    @Override
    public List<AssessmentDto> searchAll(Long patientId) {
        log.info("FHIR ASSESSMENT searchAll tenant={}, patientId={}", RequestContext.get().getTenantName(), patientId);
        // TODO
        return Collections.emptyList();
    }

    @Override
    public List<AssessmentDto> searchAll(Long patientId, Long encounterId) {
        log.info("FHIR ASSESSMENT searchAll Tenant={}, patientId={}, encounterId={}", RequestContext.get().getTenantName(), patientId, encounterId);
        // TODO
        return Collections.emptyList();
    }
}
