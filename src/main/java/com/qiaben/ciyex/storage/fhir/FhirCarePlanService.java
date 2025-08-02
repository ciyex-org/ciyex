package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirCarePlanService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch a list (bundle) of CarePlans based on query parameters
    public Bundle getCarePlans(Map<String, String> queryParams) {
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/CarePlan";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            String finalUrl = builder.build(true).toUriString();
            log.info("[FhirCarePlanService] Fetching CarePlans for org: {}, url: {}, params: {}",
                    fhirConfig.getClientId(), finalUrl, queryParams);

            String response = restClient
                    .get()
                    .uri(finalUrl)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirCarePlanService] FHIR CarePlan bundle response: {}",
                    response != null ? response.substring(0, Math.min(400, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);
            log.info("[FhirCarePlanService] Parsed {} CarePlan entries from bundle.", bundle.getEntry().size());

            return bundle;
        } catch (Exception e) {
            log.error("[FhirCarePlanService] Failed to fetch CarePlans, params: {}", queryParams, e);
            throw new RuntimeException("Failed to fetch CarePlans", e);
        }
    }

    // Fetch a single CarePlan by UUID
    public CarePlan getCarePlan(String uuid) {
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String url = fhirConfig.getApiUrl() + "/CarePlan/" + uuid;

            log.info("[FhirCarePlanService] Fetching CarePlan by UUID for org: {}, url: {}",
                    fhirConfig.getClientId(), url);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirCarePlanService] FHIR CarePlan response: {}",
                    response != null ? response.substring(0, Math.min(400, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            CarePlan carePlan = parser.parseResource(CarePlan.class, response);

            log.info("[FhirCarePlanService] Successfully parsed CarePlan for UUID: {}", uuid);
            return carePlan;
        } catch (Exception e) {
            log.error("[FhirCarePlanService] Failed to fetch CarePlan by UUID: {}", uuid, e);
            throw new RuntimeException("Failed to fetch CarePlan by UUID", e);
        }
    }
}
