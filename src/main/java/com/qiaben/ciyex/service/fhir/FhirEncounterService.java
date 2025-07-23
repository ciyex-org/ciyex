package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
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
    private final OpenEmrAuthService openEmrAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch all encounters for the patient (FHIR-standard endpoint)
    public Bundle getEncounters(Map<String, String> queryParams) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String baseUrl = openEmrConfig.getApiUrl() + "/fhir/Encounter";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            url = builder.build(true).toUriString();
            log.info("[FhirEncounterService] Fetching Encounters for org={}, clientId={}, url={}, queryParams={}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, queryParams);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
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
            log.error("[FhirEncounterService] Error fetching Encounters (org={}, clientId={}, url={}): {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    openEmrConfig != null ? openEmrConfig.getClientId() : null,
                    url, e.getMessage(), e);
            return new Bundle(); // Return an empty bundle on error
        }
    }

    // Fetch a specific encounter by UUID
    public Encounter getEncounterByUuid(String euuid) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Encounter/" + euuid;

            log.info("[FhirEncounterService] Fetching Encounter by UUID: org={}, clientId={}, url={}, uuid={}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, euuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
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
            log.error("[FhirEncounterService] Error fetching Encounter by UUID (org={}, clientId={}, url={}, uuid={}): {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    openEmrConfig != null ? openEmrConfig.getClientId() : null,
                    url, euuid, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
