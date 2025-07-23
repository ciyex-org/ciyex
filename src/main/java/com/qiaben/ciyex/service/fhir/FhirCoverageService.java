package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirCoverageService {

    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Search for coverage (returns a Bundle)
    public Bundle getCoverage(String id, String lastUpdated, String patient, String payor) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);

            url = UriComponentsBuilder.fromHttpUrl(openEmrConfig.getApiUrl() + "/fhir/Coverage")
                    .queryParamIfPresent("_id", id != null ? java.util.Optional.of(id) : java.util.Optional.empty())
                    .queryParamIfPresent("_lastUpdated", lastUpdated != null ? java.util.Optional.of(lastUpdated) : java.util.Optional.empty())
                    .queryParamIfPresent("patient", patient != null ? java.util.Optional.of(patient) : java.util.Optional.empty())
                    .queryParamIfPresent("payor", payor != null ? java.util.Optional.of(payor) : java.util.Optional.empty())
                    .toUriString();

            log.info("[FhirCoverageService] Fetching coverage for org={}, clientId={}, url={}, params: id={}, lastUpdated={}, patient={}, payor={}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, id, lastUpdated, patient, payor);

            String response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirCoverageService] Coverage bundle response: {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);

            log.info("[FhirCoverageService] Parsed {} coverage entries from bundle.", bundle.getEntry().size());
            return bundle;
        } catch (Exception e) {
            log.error("[FhirCoverageService] Error getting coverage (org={}, clientId={}, url={}, id={}, lastUpdated={}, patient={}, payor={})",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    openEmrConfig != null ? openEmrConfig.getClientId() : null,
                    url, id, lastUpdated, patient, payor, e);
            throw new RuntimeException("Failed to fetch coverage", e);
        }
    }

    // Get single coverage by FHIR UUID
    public Coverage getCoverageByUuid(String uuid) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Coverage/" + uuid;

            log.info("[FhirCoverageService] Fetching coverage by UUID for org={}, clientId={}, url={}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url);

            String response = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirCoverageService] FHIR Coverage response: {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            Coverage coverage = parser.parseResource(Coverage.class, response);

            log.info("[FhirCoverageService] Successfully parsed Coverage for UUID={}", uuid);
            return coverage;
        } catch (Exception e) {
            log.error("[FhirCoverageService] Error getting coverage by UUID (org={}, clientId={}, url={}, uuid={})",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    openEmrConfig != null ? openEmrConfig.getClientId() : null,
                    url, uuid, e);
            throw new RuntimeException("Failed to fetch coverage by UUID", e);
        }
    }
}
