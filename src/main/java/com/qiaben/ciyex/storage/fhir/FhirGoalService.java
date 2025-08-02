package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Goal;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirGoalService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch all Goal resources as a FHIR Bundle (collection)
    public Bundle getGoals(Map<String, String> queryParams) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/Goal";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            url = builder.build(true).toUri().toString();
            log.info("[FhirGoalService] Fetching FHIR Goal Bundle for clientId: {}, url: {}, params: {}",
                    fhirConfig.getClientId(), url, queryParams);

            String responseBody = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirGoalService] FHIR Goal Bundle response: {}", responseBody);
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirGoalService] Error fetching FHIR Goal Bundle for url: {}, params: {}, message: {}",
                    url, queryParams, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch goals", e);
        }
    }

    // Fetch a single Goal by UUID
    public Goal getGoalByUuid(String uuid) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Goal/" + uuid;

            log.info("[FhirGoalService] Fetching FHIR Goal by UUID for clientId: {}, url: {}, uuid: {}",
                    fhirConfig.getClientId(), url, uuid);

            String responseBody = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirGoalService] FHIR Goal (uuid={}) response: {}", uuid, responseBody);
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Goal.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirGoalService] Error fetching FHIR Goal by UUID for url: {}, uuid: {}, message: {}",
                    url, uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Goal by UUID", e);
        }
    }
}
