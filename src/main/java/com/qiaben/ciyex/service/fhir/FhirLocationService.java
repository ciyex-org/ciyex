package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Location;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirLocationService {

    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch a list of locations (FHIR Bundle)
    public Bundle getLocations(Map<String, String> queryParams) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/Location";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            url = builder.build(true).toUriString();
            log.info("[FhirLocationService] Fetching Location Bundle: url={}, params={}", url, queryParams);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirLocationService] FHIR Location Bundle response: {}", response);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, response);
        } catch (Exception e) {
            log.error("[FhirLocationService] Error fetching locations: url={}, params={}, message={}", url, queryParams, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch locations", e);
        }
    }

    // Fetch a single Location resource by UUID
    public Location getLocation(String uuid) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Location/" + uuid;

            log.info("[FhirLocationService] Fetching Location by UUID: url={}, uuid={}", url, uuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isEmpty()) {
                log.warn("[FhirLocationService] Empty response for Location UUID: {}", uuid);
                return null;
            }

            log.debug("[FhirLocationService] FHIR Location (uuid={}) response: {}", uuid, response);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Location.class, response);
        } catch (Exception e) {
            log.error("[FhirLocationService] Error fetching Location by UUID: url={}, uuid={}, message={}", url, uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Location", e);
        }
    }
}
