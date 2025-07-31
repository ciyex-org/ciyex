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
public class FhirExportService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;

    /**
     * Fetches FHIR bulk export data from FHIR server.
     * The /$export endpoint may respond asynchronously, in which case you'll need to poll the returned Content-Location URL for status/results.
     */
    public ResponseEntity<Object> getBulkExportData() {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/$export";
            log.info("[FhirExportService] Starting FHIR bulk export: clientId={}, url={}",
                    fhirConfig.getClientId(), url);

            Object response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            log.debug("[FhirExportService] Bulk export response: {}", response != null ? response.toString() : "null");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[FhirExportService] Error during FHIR bulk export: clientId={}, url={}, message={}",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch bulk export data", e);
        }
    }
}
