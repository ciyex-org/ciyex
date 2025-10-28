package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.GlobalCodeDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalGlobalCodeStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalGlobalCodeStorage implements ExternalGlobalCodeStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(GlobalCodeDto dto) {
        log.info("FHIR GlobalCode create: org={}, type={}, code={}",
                RequestContext.get().getTenantName(), dto.getCodeType(), dto.getCode());
        return null;
    }

    @Override
    public void update(String externalId, GlobalCodeDto dto) {
        log.info("FHIR GlobalCode update: id={}, org={}, type={}, code={}",
                externalId, RequestContext.get().getTenantName(), dto.getCodeType(), dto.getCode());
    }

    @Override
    public Optional<GlobalCodeDto> get(String externalId) {
        log.info("FHIR GlobalCode get: id={}", externalId);
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) {
        log.info("FHIR GlobalCode delete: id={}", externalId);
    }

    @Override
    public List<GlobalCodeDto> searchAll(Long patientId, Long encounterId,
                                   String codeType, Boolean active, String q) {
        log.info("FHIR GlobalCode search: org={}, patient={}, encounter={}, type={}, active={}, q={}",
                RequestContext.get().getTenantName(), patientId, encounterId, codeType, active, q);
        return Collections.emptyList();
    }
}
