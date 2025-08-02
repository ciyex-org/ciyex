package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirConditionService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch a list/search of Condition resources
    public Bundle getConditions(String id, String lastUpdated, String patient) {
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String url = UriComponentsBuilder.fromHttpUrl(fhirConfig.getApiUrl())
                    .path("/Condition")
                    .queryParam("_id", id)
                    .queryParam("_lastUpdated", lastUpdated)
                    .queryParam("patient", patient)
                    .toUriString();

            log.info("[FhirConditionService] Fetching conditions for org: {}, url: {}, params: id={}, lastUpdated={}, patient={}",
                    fhirConfig.getClientId(), url, id, lastUpdated, patient);

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirConditionService] FHIR Condition bundle response: {}",
                    response != null ? response.substring(0, Math.min(400, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);

            log.info("[FhirConditionService] Parsed {} Condition entries from bundle.", bundle.getEntry().size());
            return bundle;
        } catch (Exception e) {
            log.error("[FhirConditionService] Failed to fetch conditions. id={}, lastUpdated={}, patient={}", id, lastUpdated, patient, e);
            throw new RuntimeException("Failed to fetch conditions", e);
        }
    }

    // Fetch a single Condition by UUID
    public Condition getConditionByUuid(String uuid) {
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String url = UriComponentsBuilder.fromHttpUrl(fhirConfig.getApiUrl())
                    .path("/Condition/" + uuid)
                    .toUriString();

            log.info("[FhirConditionService] Fetching Condition by UUID for org: {}, url: {}", fhirConfig.getClientId(), url);

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirConditionService] FHIR Condition response: {}",
                    response != null ? response.substring(0, Math.min(400, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            Condition condition = parser.parseResource(Condition.class, response);

            log.info("[FhirConditionService] Successfully parsed Condition for UUID: {}", uuid);
            return condition;
        } catch (Exception e) {
            log.error("[FhirConditionService] Failed to fetch Condition by UUID: {}", uuid, e);
            throw new RuntimeException(e);
        }
    }
}
