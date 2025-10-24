package com.qiaben.ciyex.storage.fhir;

import com.qiaben.ciyex.dto.SocialHistoryDto;
import com.qiaben.ciyex.provider.FhirClientProvider;
import com.qiaben.ciyex.storage.ExternalSocialHistoryStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FhirExternalSocialHistoryStorage implements ExternalSocialHistoryStorage {

    private final FhirClientProvider fhirClientProvider;

    @Override
    public String create(SocialHistoryDto dto) {
        // Map entries to FHIR (e.g., Observation with category=social-history or QuestionnaireResponse)
        // var client = fhirClientProvider.getForCurrentTenant();
        return null; // return created external/bundle id
    }

    @Override
    public void update(String externalId, SocialHistoryDto dto) {
        // read + modify + update FHIR resources
    }

    @Override
    public Optional<SocialHistoryDto> get(String externalId) {
        return Optional.empty();
    }

    @Override
    public void delete(String externalId) { }

    @Override
    public List<SocialHistoryDto> searchAll(Long orgId, Long patientId) {
        return Collections.emptyList();
    }

    @Override
    public List<SocialHistoryDto> searchAll(Long orgId, Long patientId, Long encounterId) {
        return Collections.emptyList();
    }
}
