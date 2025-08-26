package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.CodeDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalCodeStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalCodeStorage implements ExternalCodeStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(CodeDto dto) {
        log.info("FHIR Code create at encounter: org={}, patient={}, encounter={}, type={}, code={}",
                dto.getOrgId(), dto.getPatientId(), dto.getEncounterId(), dto.getCodeType(), dto.getCode());
        // Map to FHIR CodeSystem/ValueSet/ConceptMap as appropriate
        return null;
    }

    @Override public void update(String externalId, CodeDto dto) { }
    @Override public Optional<CodeDto> get(String externalId) { return Optional.empty(); }
    @Override public void delete(String externalId) { }

    @Override
    public List<CodeDto> searchAll(Long orgId, Long patientId, Long encounterId,
                                   String codeType, Boolean active, String q) {
        return Collections.emptyList();
    }
}
