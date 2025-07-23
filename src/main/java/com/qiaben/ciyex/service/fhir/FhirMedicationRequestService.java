package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirMedicationRequestService {

    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch all MedicationRequest resources as a FHIR Bundle
    public Bundle getMedicationRequests(Map<String, String> queryParams) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String baseUrl = openEmrConfig.getApiUrl() + "/fhir/MedicationRequest";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            url = builder.build(true).toUriString();
            log.info("[FhirMedicationRequestService] Fetching MedicationRequests for org: {}, clientId: {}, url: {}, params: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, queryParams);

            String responseBody = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirMedicationRequestService] FHIR MedicationRequest Bundle response: {}", responseBody);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirMedicationRequestService] Failed to fetch MedicationRequests for org: {}, url: {}, params: {}, error: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    url, queryParams, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch MedicationRequests", e);
        }
    }

    // Fetch a single MedicationRequest by UUID
    public MedicationRequest getMedicationRequestByUuid(String uuid) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/MedicationRequest/" + uuid;

            log.info("[FhirMedicationRequestService] Fetching MedicationRequest by UUID for org: {}, clientId: {}, url: {}, uuid: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, uuid);

            String responseBody = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirMedicationRequestService] FHIR MedicationRequest (uuid={}) response: {}", uuid, responseBody);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(MedicationRequest.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirMedicationRequestService] Failed to fetch MedicationRequest by UUID for org: {}, url: {}, uuid: {}, error: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    url, uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch MedicationRequest by UUID", e);
        }
    }
}
