package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
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
    private final OpenEmrAuthService openEmrAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;

    /**
     * Fetches FHIR bulk export data from OpenEMR.
     * The /fhir/$export endpoint may respond asynchronously, in which case you'll need to poll the returned Content-Location URL for status/results.
     */
    public ResponseEntity<Object> getBulkExportData() {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/$export";
            log.info("[FhirExportService] Starting FHIR bulk export for org: {}, clientId: {}, url: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url);

            Object response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            log.debug("[FhirExportService] Bulk export response: {}", response != null ? response.toString() : "null");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[FhirExportService] Error during FHIR bulk export for org: {}, clientId: {}, url: {}, message: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    openEmrConfig != null ? openEmrConfig.getClientId() : null,
                    url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch bulk export data", e);
        }
    }
}
