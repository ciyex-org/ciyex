package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.CodeTypeDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalCodeTypeStorage implements ExternalStorage<CodeTypeDto> {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(CodeTypeDto dto) {
        log.info("FHIR CodeType create: org={}, patient={}, encounter={}, key={}, id={}, label={}",
                RequestContext.get().getTenantName(), dto.getPatientId(), dto.getEncounterId(),
                dto.getCodeTypeKey(), dto.getCodeTypeId(), dto.getLabel());
        // TODO: Map dto -> FHIR CodeSystem or ValueSet
        return null; // return generated externalId
    }

    @Override
    public void update(CodeTypeDto dto, String externalId) {
        log.info("FHIR CodeType update: externalId={}, org={}, patient={}, encounter={}, key={}, id={}, label={}",
                externalId, RequestContext.get().getTenantName(), dto.getPatientId(), dto.getEncounterId(),
                dto.getCodeTypeKey(), dto.getCodeTypeId(), dto.getLabel());
        // TODO: implement update
    }

    @Override
    public CodeTypeDto get(String externalId) {
        log.info("FHIR CodeType get: externalId={}", externalId);
        // TODO: fetch from FHIR server
        return null;
    }

    @Override
    public void delete(String externalId) {
        log.info("FHIR CodeType delete: externalId={}", externalId);
        // TODO: delete from FHIR server
    }

    @Override
    public List<CodeTypeDto> searchAll() {
        log.info("FHIR CodeType searchAll");
        // TODO: fetch list from FHIR server
        return Collections.emptyList();
    }

    @Override
    public boolean supports(Class<?> entityType) {
        return CodeTypeDto.class.isAssignableFrom(entityType);
    }
}
