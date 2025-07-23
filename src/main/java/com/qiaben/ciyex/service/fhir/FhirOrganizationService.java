package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirOrganizationService {

    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;
    private final FhirContext fhirContext = FhirContext.forR4();

    public Bundle getOrganizations(
            String _id, String _lastUpdated, String name, String email,
            String phone, String telecom, String address, String addressCity,
            String addressPostalCode, String addressState
    ) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(openEmrConfig.getApiUrl() + "/fhir/Organization")
                    .queryParam("_id", _id)
                    .queryParam("_lastUpdated", _lastUpdated)
                    .queryParam("name", name)
                    .queryParam("email", email)
                    .queryParam("phone", phone)
                    .queryParam("telecom", telecom)
                    .queryParam("address", address)
                    .queryParam("address-city", addressCity)
                    .queryParam("address-postalcode", addressPostalCode)
                    .queryParam("address-state", addressState);

            url = builder.toUriString();
            log.info("[FhirOrganizationService] Fetching organizations with URL: {}", url);

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirOrganizationService] Response received: {}", response);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, response);
        } catch (Exception e) {
            log.error("[FhirOrganizationService] Failed to fetch organizations from URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Organization postOrganization(Organization organization) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Organization";

            IParser parser = fhirContext.newJsonParser();
            String orgJson = parser.encodeResourceToString(organization);

            log.info("[FhirOrganizationService] Creating organization at URL: {}", url);
            log.debug("[FhirOrganizationService] Organization payload: {}", orgJson);

            String response = restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(orgJson)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirOrganizationService] Organization creation response: {}", response);

            return parser.parseResource(Organization.class, response);
        } catch (Exception e) {
            log.error("[FhirOrganizationService] Failed to create organization at URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Organization getOrganizationByUuid(String uuid) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Organization/" + uuid;

            log.info("[FhirOrganizationService] Fetching organization by UUID {} from URL: {}", uuid, url);

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirOrganizationService] Organization fetch response: {}", response);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Organization.class, response);
        } catch (Exception e) {
            log.error("[FhirOrganizationService] Failed to fetch organization by UUID {} from URL: {}. Error: {}", uuid, url, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Organization updateOrganization(String uuid, Organization updatedOrganization) {
        OpenEmrConfig openEmrConfig = null;
        String url = null;
        try {
            openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Organization/" + uuid;

            IParser parser = fhirContext.newJsonParser();
            String orgJson = parser.encodeResourceToString(updatedOrganization);

            log.info("[FhirOrganizationService] Updating organization UUID {} at URL: {}", uuid, url);
            log.debug("[FhirOrganizationService] Updated organization payload: {}", orgJson);

            String response = restClient.put()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(orgJson)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirOrganizationService] Organization update response: {}", response);

            return parser.parseResource(Organization.class, response);
        } catch (Exception e) {
            log.error("[FhirOrganizationService] Failed to update organization UUID {} at URL: {}. Error: {}", uuid, url, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
