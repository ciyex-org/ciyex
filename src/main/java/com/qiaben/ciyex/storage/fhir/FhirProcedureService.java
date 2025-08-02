package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Procedure;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirProcedureService {

    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirAuthService fhirAuthService;
    private final RestClient restClient;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Search procedures with FHIR params
    public Bundle getProcedures(Map<String, String> params) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Procedure"; // Add /fhir/ if needed
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

            params.forEach((key, value) -> {
                if (value != null && !value.isBlank()) builder.queryParam(key, value);
            });

            String fullUrl = builder.build(true).toUriString();
            log.info("[FhirProcedureService] Searching procedures from URL: {}", fullUrl);

            String json = restClient.get()
                    .uri(fullUrl)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirProcedureService] Response: {}", json);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, json);
        } catch (Exception e) {
            log.error("[FhirProcedureService] Failed to fetch procedures from URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch procedures", e);
        }
    }

    // Get a single Procedure by UUID
    public Procedure getProcedureById(String uuid) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Procedure/" + uuid; // Add /fhir/ if needed
            log.info("[FhirProcedureService] Fetching procedure by ID {} from URL: {}", uuid, url);

            String json = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirProcedureService] Response for procedure {}: {}", uuid, json);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Procedure.class, json);
        } catch (Exception e) {
            log.error("[FhirProcedureService] Failed to fetch procedure by ID {} from URL: {}. Error: {}", uuid, url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch procedure by UUID: " + uuid, e);
        }
    }
}
