package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirImmunizationService {

    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch a list of Immunizations (as Bundle)
    public Bundle getImmunizations(Map<String, String> queryParams) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String baseUrl = openEmrConfig.getApiUrl() + "/fhir/Immunization";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            url = builder.build(true).toUriString();
            log.info("[FhirImmunizationService] Fetching Immunization Bundle for org: {}, clientId: {}, url: {}, params: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, queryParams);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirImmunizationService] FHIR Immunization Bundle response: {}", response);
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, response);
        } catch (Exception e) {
            log.error("[FhirImmunizationService] Error fetching Immunization Bundle for org: {}, url: {}, params: {}, message: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    url, queryParams, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch immunizations", e);
        }
    }

    // Fetch a single Immunization by UUID
    public Immunization getImmunization(String uuid) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Immunization/" + uuid;
            log.info("[FhirImmunizationService] Fetching Immunization by UUID for org: {}, clientId: {}, url: {}, uuid: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, uuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirImmunizationService] FHIR Immunization (uuid={}) response: {}", uuid, response);
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Immunization.class, response);
        } catch (Exception e) {
            log.error("[FhirImmunizationService] Error fetching Immunization by UUID for org: {}, url: {}, uuid: {}, message: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    url, uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Immunization by UUID", e);
        }
    }
}
