package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig; // <- UPDATED
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ValueSet;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirValueSetService {

    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final RestClient restClient;
    private final FhirAuthService fhirAuthService; // <- UPDATED
    private final FhirContext fhirContext = FhirContext.forR4();

    public Bundle getValueSets(Map<String, String> params) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR); // <- UPDATED
            url = fhirConfig.getApiUrl() + "/fhir/ValueSet";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

            params.forEach((k, v) -> {
                if (v != null && !v.isBlank()) builder.queryParam(k, v);
            });

            String finalUrl = builder.build(true).toUriString();
            log.info("[FhirValueSetService] Fetching ValueSets from URL: {}", finalUrl);

            String response = restClient
                    .get()
                    .uri(finalUrl)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken()) // <- UPDATED
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirValueSetService] Response received: {}", response);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, response);
        } catch (Exception e) {
            log.error("[FhirValueSetService] Failed to fetch ValueSets. Error: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch ValueSets", e);
        }
    }

    public ValueSet getValueSetById(String uuid) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR); // <- UPDATED
            url = fhirConfig.getApiUrl() + "/fhir/ValueSet/" + uuid;

            log.info("[FhirValueSetService] Fetching ValueSet by id: {} from URL: {}", uuid, url);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken()) // <- UPDATED
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirValueSetService] Response received for id {}: {}", uuid, response);

            if (response == null || response.isEmpty()) {
                log.warn("[FhirValueSetService] No ValueSet found for id: {}", uuid);
                return null;
            }

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(ValueSet.class, response);
        } catch (Exception e) {
            log.error("[FhirValueSetService] Failed to fetch ValueSet by id: {}. Error: {}", uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch ValueSet by id", e);
        }
    }
}
