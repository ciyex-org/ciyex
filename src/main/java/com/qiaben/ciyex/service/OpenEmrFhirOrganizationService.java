package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.OrganizationRequestDTO;  // Use OrganizationRequestDTO for POST/PUT requests
import com.qiaben.ciyex.dto.OrganizationResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class OpenEmrFhirOrganizationService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;

    @Autowired
    public OpenEmrFhirOrganizationService(OpenEmrFhirProperties openEmrFhirProperties, RestClient restClient) {
        this.openEmrFhirProperties = openEmrFhirProperties;
        this.restClient = restClient;
    }

    public List<OrganizationResponseDTO> getOrganizations(
            String _id, String _lastUpdated, String name, String email,
            String phone, String telecom, String address, String addressCity,
            String addressPostalCode, String addressState
    ) {
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

        //  This is already synchronous. No need for `.block()`.
        ResponseEntity<List> response = restClient.get()
                .uri(url)
                .retrieve()
                .toEntity(List.class); // This is the final call

        return response.getBody(); // Safely returns the body
    }

    // Updated to use OrganizationRequestDTO for POST requests
    public OrganizationResponseDTO postOrganization(OrganizationRequestDTO organizationRequestDTO) {
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Organization";

        return restClient.post()
                .uri(url)
                .body(organizationRequestDTO)  // Send the request body as OrganizationRequestDTO
                .retrieve()
                .body(OrganizationResponseDTO.class);  // The response is still OrganizationResponseDTO
    }

    public OrganizationResponseDTO getOrganizationByUuid(String uuid) {
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Organization/" + uuid;

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(OrganizationResponseDTO.class); // Synchronous, blocking call
    }

    // Updated to use OrganizationRequestDTO for PUT requests
    public OrganizationResponseDTO updateOrganization(String uuid, OrganizationRequestDTO updatedOrganizationRequestDTO) {
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Organization/" + uuid;

        return restClient.put()
                .uri(url)
                .body(updatedOrganizationRequestDTO)  // Send the request body as OrganizationRequestDTO
                .retrieve()
                .body(OrganizationResponseDTO.class); // The response is still OrganizationResponseDTO
    }
}
