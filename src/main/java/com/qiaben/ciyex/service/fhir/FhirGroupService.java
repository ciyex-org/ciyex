package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
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
    private final FhirAuthService fhirAuthService;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch Group resources by query parameters (returns a Bundle)
    public Bundle getGroup(Map<String, String> queryParams) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/Group";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            url = builder.build(true).toUri().toString();
            log.info("[FhirGroupService] Fetching FHIR Group Bundle for clientId: {}, url: {}, params: {}",
                    fhirConfig.getClientId(), url, queryParams);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirGroupService] FHIR Group Bundle response: {}", response);
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, response);
        } catch (Exception e) {
            log.error("[FhirGroupService] Error fetching FHIR Group Bundle url: {}, params: {}, message: {}",
                    url, queryParams, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch groups", e);
        }
    }

    // Fetch a single Group by UUID
    public Group getGroupByUuid(String uuid) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Group/" + uuid;
            log.info("[FhirGroupService] Fetching FHIR Group by UUID: url: {}, uuid: {}", url, uuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirGroupService] FHIR Group (uuid={}) response: {}", uuid, response);
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Group.class, response);
        } catch (Exception e) {
            log.error("[FhirGroupService] Error fetching FHIR Group by UUID url: {}, uuid: {}, message: {}",
                    url, uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch group by UUID", e);
        }
    }

    // Export group by id (returns raw FHIR server response as String)
    public ResponseEntity<String> exportGroup(String id) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Group/" + id + "/$export";
            log.info("[FhirGroupService] Exporting FHIR Group by id: url: {}, id: {}", url, id);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirGroupService] FHIR Group export (id={}) response: {}", id, response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[FhirGroupService] Error exporting FHIR Group url: {}, id: {}, message: {}",
                    url, id, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
