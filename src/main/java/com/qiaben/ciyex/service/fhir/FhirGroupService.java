package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirGroupService {

    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch Group resources by query parameters (returns a Bundle)
    public Bundle getGroup(Map<String, String> queryParams) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String baseUrl = openEmrConfig.getApiUrl() + "/fhir/Group";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            url = builder.build(true).toUri().toString();
            log.info("[FhirGroupService] Fetching FHIR Group Bundle for org: {}, clientId: {}, url: {}, params: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, queryParams);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirGroupService] FHIR Group Bundle response: {}", response);
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, response);
        } catch (Exception e) {
            log.error("[FhirGroupService] Error fetching FHIR Group Bundle for org: {}, url: {}, params: {}, message: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    url, queryParams, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch groups", e);
        }
    }

    // Fetch a single Group by UUID
    public Group getGroupByUuid(String uuid) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Group/" + uuid;
            log.info("[FhirGroupService] Fetching FHIR Group by UUID for org: {}, clientId: {}, url: {}, uuid: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, uuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirGroupService] FHIR Group (uuid={}) response: {}", uuid, response);
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Group.class, response);
        } catch (Exception e) {
            log.error("[FhirGroupService] Error fetching FHIR Group by UUID for org: {}, url: {}, uuid: {}, message: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    url, uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch group by UUID", e);
        }
    }

    // Export group by id (returns raw FHIR server response as String)
    public ResponseEntity<String> exportGroup(String id) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Group/" + id + "/$export";
            log.info("[FhirGroupService] Exporting FHIR Group by id for org: {}, clientId: {}, url: {}, id: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, id);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirGroupService] FHIR Group export (id={}) response: {}", id, response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[FhirGroupService] Error exporting FHIR Group for org: {}, url: {}, id: {}, message: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    url, id, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
