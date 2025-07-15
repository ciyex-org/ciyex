package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.OrganizationRequestDTO;
import com.qiaben.ciyex.dto.fhir.OrganizationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenEmrFhirOrganizationService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public List<OrganizationResponseDTO> getOrganizations(
            String _id, String _lastUpdated, String name, String email,
            String phone, String telecom, String address, String addressCity,
            String addressPostalCode, String addressState
    ) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(openEmrFhirProperties.getBaseUrl() + "/fhir/Organization")
                    .queryParam("_id", _id)
                    .queryParam("_lastUpdated", _lastUpdated)
                    .queryParam("name", name)
                    .queryParam("email", email)
                    .queryParam("phone", phone)
                    .queryParam("telecom", telecom)
                    .queryParam("address", address)
                    .queryParam("address-city", addressCity)
                    .queryParam("address-postalcode", addressPostalCode)
                    .queryParam("address-state", addressState)
                    .toUriString();

            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<OrganizationResponseDTO>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OrganizationResponseDTO postOrganization(OrganizationRequestDTO organizationRequestDTO) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Organization";

            return restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(organizationRequestDTO)
                    .retrieve()
                    .body(OrganizationResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OrganizationResponseDTO getOrganizationByUuid(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Organization/" + uuid;

            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(OrganizationResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public OrganizationResponseDTO updateOrganization(String uuid, OrganizationRequestDTO updatedOrganizationRequestDTO) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Organization/" + uuid;

            return restClient.put()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(updatedOrganizationRequestDTO)
                    .retrieve()
                    .body(OrganizationResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
