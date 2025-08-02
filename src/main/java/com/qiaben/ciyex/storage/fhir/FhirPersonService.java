package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Person;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirPersonService {

    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final RestClient restClient;
    private final FhirContext fhirContext = FhirContext.forR4();

    public Bundle getPersons(Map<String, String> searchParams) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/Person";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            searchParams.forEach((key, value) -> {
                if (value != null && !value.isBlank()) builder.queryParam(key, value);
            });

            url = builder.build(true).toUriString();
            log.info("[FhirPersonService] Fetching persons from URL: {}", url);

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPersonService] Persons response: {}", response);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, response);
        } catch (Exception e) {
            log.error("[FhirPersonService] Failed to fetch persons from URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch persons", e);
        }
    }

    public Person getPersonById(String uuid) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Person/" + uuid;
            log.info("[FhirPersonService] Fetching person by ID {} from URL: {}", uuid, url);

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPersonService] Person response: {}", response);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Person.class, response);
        } catch (Exception e) {
            log.error("[FhirPersonService] Failed to fetch person by ID {} from URL: {}. Error: {}", uuid, url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch person by ID: " + uuid, e);
        }
    }
}
