package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirPractitionerService {

    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Search practitioners by FHIR params
    public Bundle getPractitioners(Map<String, String> params) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Practitioner";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

            params.forEach((key, value) -> {
                if (value != null && !value.isBlank()) builder.queryParam(key, value);
            });

            String fullUrl = builder.build(true).toUriString();
            log.info("[FhirPractitionerService] Fetching practitioners from URL: {}", fullUrl);

            String json = restClient
                    .get()
                    .uri(fullUrl)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPractitionerService] Response: {}", json);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, json);
        } catch (Exception e) {
            log.error("[FhirPractitionerService] Failed to fetch practitioners from URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch practitioners", e);
        }
    }

    // Get by id (UUID)
    public Practitioner getPractitionerById(String uuid) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Practitioner/" + uuid;
            log.info("[FhirPractitionerService] Fetching practitioner by ID {} from URL: {}", uuid, url);

            String json = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPractitionerService] Response for practitioner {}: {}", uuid, json);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Practitioner.class, json);
        } catch (Exception e) {
            log.error("[FhirPractitionerService] Failed to fetch practitioner by ID {} from URL: {}. Error: {}", uuid, url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch practitioner by ID: " + uuid, e);
        }
    }

    // Create
    public Practitioner createPractitioner(Practitioner practitioner) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Practitioner";
            IParser parser = fhirContext.newJsonParser();
            String payload = parser.encodeResourceToString(practitioner);

            log.info("[FhirPractitionerService] Creating practitioner at URL: {}", url);
            log.debug("[FhirPractitionerService] Payload: {}", payload);

            String json = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPractitionerService] Response after create: {}", json);
            return parser.parseResource(Practitioner.class, json);
        } catch (Exception e) {
            log.error("[FhirPractitionerService] Failed to create practitioner at URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to create practitioner", e);
        }
    }

    // Update
    public Practitioner updatePractitioner(String uuid, Practitioner practitioner) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Practitioner/" + uuid;
            IParser parser = fhirContext.newJsonParser();
            String payload = parser.encodeResourceToString(practitioner);

            log.info("[FhirPractitionerService] Updating practitioner {} at URL: {}", uuid, url);
            log.debug("[FhirPractitionerService] Payload: {}", payload);

            String json = restClient
                    .method(HttpMethod.PUT)
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPractitionerService] Response after update: {}", json);
            return parser.parseResource(Practitioner.class, json);
        } catch (Exception e) {
            log.error("[FhirPractitionerService] Failed to update practitioner {} at URL: {}. Error: {}", uuid, url, e.getMessage(), e);
            throw new RuntimeException("Failed to update practitioner", e);
        }
    }
}
