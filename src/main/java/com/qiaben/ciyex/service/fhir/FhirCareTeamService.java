package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CareTeam;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirCareTeamService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch all CareTeam resources as a FHIR Bundle
    public Bundle getCareTeams(Map<String, String> queryParams) {
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/CareTeam";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((k, v) -> {
                if (v != null && !v.isBlank()) {
                    builder.queryParam(k, v);
                }
            });

            String finalUrl = builder.build(true).toUriString();
            log.info("[FhirCareTeamService] Fetching CareTeams for org: {}, url: {}, params: {}",
                    fhirConfig.getClientId(), finalUrl, queryParams);

            String response = restClient
                    .get()
                    .uri(finalUrl)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirCareTeamService] FHIR CareTeam bundle response: {}",
                    response != null ? response.substring(0, Math.min(400, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);
            log.info("[FhirCareTeamService] Parsed {} CareTeam entries from bundle.", bundle.getEntry().size());

            return bundle;
        } catch (Exception e) {
            log.error("[FhirCareTeamService] Failed to fetch CareTeams, params: {}", queryParams, e);
            throw new RuntimeException("Failed to fetch CareTeams", e);
        }
    }

    // Fetch a single CareTeam by UUID
    public CareTeam getCareTeamByUuid(String uuid) {
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String url = fhirConfig.getApiUrl() + "/CareTeam/" + uuid;

            log.info("[FhirCareTeamService] Fetching CareTeam by UUID for org: {}, url: {}",
                    fhirConfig.getClientId(), url);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirCareTeamService] FHIR CareTeam response: {}",
                    response != null ? response.substring(0, Math.min(400, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            CareTeam careTeam = parser.parseResource(CareTeam.class, response);

            log.info("[FhirCareTeamService] Successfully parsed CareTeam for UUID: {}", uuid);
            return careTeam;
        } catch (Exception e) {
            log.error("[FhirCareTeamService] Failed to fetch CareTeam by UUID: {}", uuid, e);
            throw new RuntimeException("Failed to fetch CareTeam by UUID", e);
        }
    }
}
