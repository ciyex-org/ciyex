package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirBulkDataStatusService {

    private final RestClient restClient;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirAuthService fhirAuthService; // <--- Injected!

    private final FhirContext fhirContext = FhirContext.forR4();

    public Resource getBulkDataStatus() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String url = fhirConfig.getApiUrl() + "/$bulkdata-status";
            log.info("[Org:{}] Requesting FHIR BulkData status from: {}", orgId, url);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[Org:{}] BulkData status response: {}", orgId,
                    response != null ? response.substring(0, Math.min(512, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            Resource resource = parser.parseResource(Resource.class, response);

            log.info("[Org:{}] Successfully parsed BulkData status as FHIR Resource.", orgId);
            return resource;
        } catch (Exception e) {
            log.error("[Org:{}] Failed to fetch bulk data status", orgId, e);
            throw new RuntimeException("Failed to fetch bulk data status", e);
        }
    }

    public Resource deleteBulkDataStatus() {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String url = fhirConfig.getApiUrl() + "/$bulkdata-status";
            log.info("[Org:{}] Deleting FHIR BulkData status at: {}", orgId, url);

            String response = restClient
                    .delete()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[Org:{}] Delete BulkData status response: {}", orgId,
                    response != null ? response.substring(0, Math.min(512, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            Resource resource = parser.parseResource(Resource.class, response);

            log.info("[Org:{}] Successfully deleted BulkData status and parsed response.", orgId);
            return resource;
        } catch (Exception e) {
            log.error("[Org:{}] Failed to delete bulk data status", orgId, e);
            throw new RuntimeException("Failed to delete bulk data status", e);
        }
    }
}
