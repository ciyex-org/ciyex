package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirEncounterService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch all encounters for the patient (FHIR-standard endpoint)
    public Bundle getEncounters(Map<String, String> queryParams) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/Encounter";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            url = builder.build(true).toUriString();
            log.info("[FhirEncounterService] Fetching Encounters: clientId={}, url={}, queryParams={}",
                    fhirConfig.getClientId(), url, queryParams);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirEncounterService] Encounter bundle response (first 400 chars): {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);

            log.info("[FhirEncounterService] Parsed {} Encounter entries from bundle.", bundle.getEntry().size());
            return bundle;
        } catch (Exception e) {
            log.error("[FhirEncounterService] Error fetching Encounters (clientId={}, url={}): {}",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, e.getMessage(), e);
            return new Bundle(); // Return an empty bundle on error
        }
    }

    // Fetch a specific encounter by UUID
    public Encounter getEncounterByUuid(String euuid) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Encounter/" + euuid;

            log.info("[FhirEncounterService] Fetching Encounter by UUID: clientId={}, url={}, uuid={}",
                    fhirConfig.getClientId(), url, euuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirEncounterService] Single Encounter response (first 400 chars): {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            Encounter encounter = parser.parseResource(Encounter.class, response);

            log.info("[FhirEncounterService] Successfully parsed Encounter with UUID: {}", euuid);
            return encounter;
        } catch (Exception e) {
            log.error("[FhirEncounterService] Error fetching Encounter by UUID (clientId={}, url={}, uuid={}): {}",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, euuid, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
