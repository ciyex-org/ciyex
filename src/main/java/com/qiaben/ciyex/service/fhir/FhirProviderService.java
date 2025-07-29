package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirProviderService {

    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirAuthService fhirAuthService;
    private final RestClient restClient;
    private final FhirContext fhirContext = FhirContext.forR4();

    /**
     * Creates a provider (Practitioner) in the FHIR server (multi-tenant aware).
     *
     * @param practitioner Practitioner resource (FHIR R4)
     * @return Created Practitioner
     */
    public Practitioner createProvider(Practitioner practitioner) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Practitioner"; // Use /fhir/Practitioner if your base URL requires it
            String token = fhirAuthService.getCachedAccessToken();

            IParser parser = fhirContext.newJsonParser();
            String practitionerJson = parser.encodeResourceToString(practitioner);

            log.info("[FhirProviderService] Creating provider at URL: {}", url);
            log.debug("[FhirProviderService] Request payload: {}", practitionerJson);

            String responseBody = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(practitionerJson)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirProviderService] Response: {}", responseBody);

            return parser.parseResource(Practitioner.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirProviderService] Failed to create provider at URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to create provider", e);
        }
    }
}
