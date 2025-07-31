package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Device;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirDeviceService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch a Bundle (list) of Devices by search parameters
    public Bundle getDevices(String _id, String _lastUpdated, String patient) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/Device";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            if (_id != null && !_id.isEmpty()) builder.queryParam("_id", _id);
            if (_lastUpdated != null && !_lastUpdated.isEmpty()) builder.queryParam("_lastUpdated", _lastUpdated);
            if (patient != null && !patient.isEmpty()) builder.queryParam("patient", patient);

            url = builder.build(true).toUri().toString();

            log.info("[FhirDeviceService] Fetching devices for clientId={}, url={}, params: _id={}, _lastUpdated={}, patient={}",
                    fhirConfig.getClientId(), url, _id, _lastUpdated, patient);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirDeviceService] Devices bundle response: {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);

            log.info("[FhirDeviceService] Parsed {} device entries from bundle.", bundle.getEntry().size());
            return bundle;
        } catch (Exception e) {
            log.error("[FhirDeviceService] Error getting devices (clientId={}, url={}, _id={}, _lastUpdated={}, patient={})",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, _id, _lastUpdated, patient, e);
            throw new RuntimeException("Failed to fetch devices", e);
        }
    }

    // Fetch a single Device by UUID
    public Device getDeviceById(String uuid) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Device/" + uuid;

            log.info("[FhirDeviceService] Fetching device by UUID for clientId={}, url={}, uuid={}",
                    fhirConfig.getClientId(), url, uuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirDeviceService] FHIR Device response: {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            Device device = parser.parseResource(Device.class, response);

            log.info("[FhirDeviceService] Successfully parsed Device for UUID={}", uuid);
            return device;
        } catch (Exception e) {
            log.error("[FhirDeviceService] Error getting device by UUID (clientId={}, url={}, uuid={})",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, uuid, e);
            throw new RuntimeException("Failed to fetch Device by UUID", e);
        }
    }
}
