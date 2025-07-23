package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
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
    private final OpenEmrAuthService openEmrAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    public Resource getBulkDataStatus() {
        try {
            OpenEmrConfig openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String url = openEmrConfig.getApiUrl() + "/fhir/$bulkdata-status";
            log.info("Requesting FHIR BulkData status from: {}", url);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("BulkData status response: {}",
                    response != null ? response.substring(0, Math.min(512, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            Resource resource = parser.parseResource(Resource.class, response);

            log.info("Successfully parsed BulkData status as FHIR Resource.");
            return resource;
        } catch (Exception e) {
            log.error("Failed to fetch bulk data status", e);
            throw new RuntimeException("Failed to fetch bulk data status", e);
        }
    }

    public Resource deleteBulkDataStatus() {
        try {
            OpenEmrConfig openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String url = openEmrConfig.getApiUrl() + "/fhir/$bulkdata-status";
            log.info("Deleting FHIR BulkData status at: {}", url);

            String response = restClient
                    .delete()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("Delete BulkData status response: {}",
                    response != null ? response.substring(0, Math.min(512, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            Resource resource = parser.parseResource(Resource.class, response);

            log.info("Successfully deleted BulkData status and parsed response.");
            return resource;
        } catch (Exception e) {
            log.error("Failed to delete bulk data status", e);
            throw new RuntimeException("Failed to delete bulk data status", e);
        }
    }
}
