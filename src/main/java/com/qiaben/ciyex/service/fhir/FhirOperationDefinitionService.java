package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirOperationDefinitionService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;

    /**
     * Fetch all FHIR OperationDefinition resources (returns a Bundle).
     */
    public ResponseEntity<Object> getAllOperationDefinitions() {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/OperationDefinition";

            log.info("[FhirOperationDefinitionService] Fetching all OperationDefinitions from URL: {}", url);

            Object response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            log.debug("[FhirOperationDefinitionService] OperationDefinitions response: {}", response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[FhirOperationDefinitionService] Failed to fetch all OperationDefinitions from URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch all OperationDefinitions", e);
        }
    }

    /**
     * Fetch a specific OperationDefinition by name or id.
     * @param operation The name or id of the OperationDefinition resource.
     */
    public ResponseEntity<Object> getOperationDefinitionByName(String operation) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/OperationDefinition/" + operation;

            log.info("[FhirOperationDefinitionService] Fetching OperationDefinition '{}' from URL: {}", operation, url);

            Object response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            log.debug("[FhirOperationDefinitionService] OperationDefinition '{}' response: {}", operation, response);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[FhirOperationDefinitionService] Failed to fetch OperationDefinition '{}' from URL: {}. Error: {}", operation, url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch OperationDefinition: " + operation, e);
        }
    }
}
