package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.ProviderSignatureDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalProviderSignatureStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalProviderSignatureStorage implements ExternalProviderSignatureStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(ProviderSignatureDto dto) {
        log.info("FHIR ProviderSignature create patient={} encounter={}", dto.getPatientId(), dto.getEncounterId());
        // Map to Provenance.signature or VerificationResult; return external id
        return null;
    }

    @Override public void update(String externalId, ProviderSignatureDto dto) { }
    @Override public Optional<ProviderSignatureDto> get(String externalId) { return Optional.empty(); }
    @Override public void delete(String externalId) { }
    @Override public List<ProviderSignatureDto> searchAll(Long orgId, Long patientId) { return Collections.emptyList(); }
    @Override public List<ProviderSignatureDto> searchAll(Long orgId, Long patientId, Long encounterId) { return Collections.emptyList(); }
}
