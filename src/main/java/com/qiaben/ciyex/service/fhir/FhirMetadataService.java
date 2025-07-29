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
public class FhirMetadataService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;

    // Fetch the FHIR /metadata endpoint (CapabilityStatement)
    public ResponseEntity<Object> getMetadata() {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/metadata";

            log.info("[FhirMetadataService] Fetching FHIR metadata from URL: {}", url);

            Object body = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            log.debug("[FhirMetadataService] FHIR metadata response: {}", body);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("[FhirMetadataService] Failed to fetch FHIR metadata from URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch FHIR metadata", e);
        }
    }

    // Fetch the SMART on FHIR well-known configuration
    public ResponseEntity<Object> getSmartConfiguration() {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/.well-known/smart-configuration";

            log.info("[FhirMetadataService] Fetching SMART configuration from URL: {}", url);

            Object body = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            log.debug("[FhirMetadataService] SMART configuration response: {}", body);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("[FhirMetadataService] Failed to fetch SMART configuration from URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch SMART configuration", e);
        }
    }
}
