package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.CodeDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
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
        log.info("FHIR Code create: org={}, type={}, code={}",
                RequestContext.get().getTenantName(), dto.getCodeType(), dto.getCode());
        return null;
    }

    @Override
    public void update(String externalId, CodeDto dto) {
        log.info("FHIR Code update: id={}, org={}, type={}, code={}",
                externalId, RequestContext.get().getTenantName(), dto.getCodeType(), dto.getCode());
    }

    @Override
    public Optional<CodeDto> get(String externalId) {
        log.info("FHIR Code get: id={}", externalId);
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) {
        log.info("FHIR Code delete: id={}", externalId);
    }

    // ✅ Match the interface signature exactly
    @Override
    public List<CodeDto> searchAll(Long patientId, Long encounterId,
                                   String codeType, Boolean active, String q) {
        log.info("FHIR Code search: org={}, patient={}, encounter={}, type={}, active={}, q={}",
                RequestContext.get().getTenantName(), patientId, encounterId, codeType, active, q);
        return Collections.emptyList();
    }
}
