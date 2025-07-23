package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Medication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirMedicationService {

    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Get medications as a Bundle (FHIR standard)
    public Bundle getMedications(String id, String lastUpdated) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            var urlBuilder = UriComponentsBuilder.fromHttpUrl(openEmrConfig.getApiUrl() + "/fhir/Medication");
            if (id != null && !id.isEmpty()) urlBuilder.queryParam("_id", id);
            if (lastUpdated != null && !lastUpdated.isEmpty()) urlBuilder.queryParam("_lastUpdated", lastUpdated);

            url = urlBuilder.toUriString();
            log.info("[FhirMedicationService] Fetching medications for org: {}, clientId: {}, URL: {}, _id: {}, _lastUpdated: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, id, lastUpdated);

            String responseBody = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirMedicationService] Medication Bundle response: {}", responseBody);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirMedicationService] Failed to fetch medications for org: {}, URL: {}, error: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Medications", e);
        }
    }

    // Get single Medication by UUID
    public Medication getMedicationByUuid(String uuid) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Medication/" + uuid;

            log.info("[FhirMedicationService] Fetching Medication by UUID for org: {}, clientId: {}, URL: {}, UUID: {}",
                    openEmrConfig.getAudience(), openEmrConfig.getClientId(), url, uuid);

            String responseBody = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirMedicationService] Medication (UUID={}) response: {}", uuid, responseBody);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Medication.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirMedicationService] Failed to fetch Medication by UUID for org: {}, URL: {}, UUID: {}, error: {}",
                    openEmrConfig != null ? openEmrConfig.getAudience() : null,
                    url, uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch Medication by UUID", e);
        }
    }
}
