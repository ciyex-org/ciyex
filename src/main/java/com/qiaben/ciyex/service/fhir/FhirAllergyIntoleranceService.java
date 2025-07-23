package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirAllergyIntoleranceService {

    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    public List<AllergyIntolerance> getAllergyIntolerances(Map<String, String> queryParams) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            OpenEmrConfig openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String baseUrl = openEmrConfig.getApiUrl() + "/fhir/AllergyIntolerance";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            log.info("[Org:{}] Fetching AllergyIntolerance list with params: {}", orgId, queryParams);

            String response = restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);

            List<AllergyIntolerance> allergyIntoleranceList = new ArrayList<>();
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.getResource() instanceof AllergyIntolerance) {
                    allergyIntoleranceList.add((AllergyIntolerance) entry.getResource());
                }
            }

            log.info("[Org:{}] Found {} AllergyIntolerance records.", orgId, allergyIntoleranceList.size());
            return allergyIntoleranceList;
        } catch (Exception e) {
            log.error("[Org:{}] Failed to fetch AllergyIntolerance list: {}", orgId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public AllergyIntolerance getAllergyIntolerance(String uuid) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            OpenEmrConfig openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String url = openEmrConfig.getApiUrl() + "/fhir/AllergyIntolerance/" + uuid;

            log.info("[Org:{}] Fetching AllergyIntolerance by UUID: {}", orgId, uuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            IParser parser = fhirContext.newJsonParser();
            AllergyIntolerance resource = parser.parseResource(AllergyIntolerance.class, response);

            log.info("[Org:{}] Successfully fetched AllergyIntolerance: {}", orgId, uuid);
            return resource;
        } catch (Exception e) {
            log.error("[Org:{}] Failed to fetch AllergyIntolerance by UUID {}: {}", orgId, uuid, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
