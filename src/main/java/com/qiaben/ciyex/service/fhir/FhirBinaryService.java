package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Binary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirBinaryService {

    private final RestClient restClient;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    // Inject your FhirAuthService or token provider here as needed.
    private final FhirAuthService fhirAuthService;

    private final FhirContext fhirContext = FhirContext.forR4();
    private static final String BINARY_URL = "/Binary/{id}";

    /**
     * Fetches a Binary FHIR resource by its FHIR id, using multi-tenant organization context.
     */
    public Binary getBinaryDocument(String id) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        log.info("Fetching Binary document for orgId {} and id: {}", orgId, id);
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String url = fhirConfig.getApiUrl() + BINARY_URL.replace("{id}", id);
            log.debug("Resolved FHIR Binary endpoint URL: {}", url);

            String accessToken = fhirAuthService.getCachedAccessToken(); // Implement this for FHIR
            log.debug("Using access token: {}", accessToken != null ? "[REDACTED]" : null);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("FHIR Binary response: {}", response != null ? response.substring(0, Math.min(256, response.length())) : "null");

            IParser parser = fhirContext.newJsonParser();
            Binary binary = parser.parseResource(Binary.class, response);
            log.info("Successfully parsed Binary resource for id: {}", id);

            return binary;
        } catch (Exception e) {
            log.error("Failed to fetch or parse FHIR Binary resource for id: {}", id, e);
            throw new RuntimeException("Failed to fetch FHIR Binary resource", e);
        }
    }
}
