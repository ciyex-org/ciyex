package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirObservationService {

    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch a list of observations as a FHIR Bundle
    public Bundle getObservations(Map<String, String> queryParams) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/Observation";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    builder.queryParam(k, v);
                }
            });

            url = builder.build(true).toUriString();
            log.info("[FhirObservationService] Fetching Observations from URL: {}", url);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirObservationService] Observations response: {}", response);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, response);
        } catch (Exception e) {
            log.error("[FhirObservationService] Failed to fetch observations from URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch observations", e);
        }
    }

    // Fetch a single Observation resource by UUID
    public Observation getObservationByUuid(String uuid) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Observation/" + uuid;

            log.info("[FhirObservationService] Fetching Observation by UUID: {} from URL: {}", uuid, url);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirObservationService] Observation response for UUID {}: {}", uuid, response);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Observation.class, response);
        } catch (Exception e) {
            log.error("[FhirObservationService] Failed to fetch observation by UUID: {} from URL: {}. Error: {}", uuid, url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch observation by UUID", e);
        }
    }
}
