package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.SignoffDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalSignoffStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalSignoffStorage implements ExternalSignoffStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(SignoffDto dto) {
        log.info("FHIR Signoff create patientId={}, target={}#{}", dto.getPatientId(), dto.getTargetType(), dto.getTargetId());
        // var client = fhirClientProvider.getForCurrentTenant();
        // Build Provenance/Composition with signature; return id
        return null;
    }

    @Override public void update(String externalId, SignoffDto dto) { /* update attestation if allowed */ }

    @Override public Optional<SignoffDto> get(String externalId) { return Optional.empty(); }

    @Override public void delete(String externalId) { /* delete or mark as retracted */ }

    @Override public List<SignoffDto> searchAll(Long orgId, Long patientId) { return Collections.emptyList(); }

    @Override public List<SignoffDto> searchAll(Long orgId, Long patientId, Long encounterId) { return Collections.emptyList(); }
}
