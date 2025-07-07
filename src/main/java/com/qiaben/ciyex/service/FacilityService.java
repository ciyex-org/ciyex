package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.FacilityRequestDTO;
import com.qiaben.ciyex.dto.FacilityResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacilityService {

    // You can configure this in application.yml as: openemr.api-url: https://your-openemr/api
    @Value("${openemr.api-url}")
    private String openEmrApiUrl;

    // If OpenEMR API needs auth, inject token here or via config
    // @Value("${openemr.api-key}") private String apiKey;

    private final RestClient restClient = RestClient.builder().build();

    /**
     * Query facilities using filter params.
     */
    public List<FacilityResponseDTO> getFacilities(FacilityRequestDTO filter) {
        String url = buildFacilityApiUrl(filter);

        try {
            ResponseEntity<FacilityResponseDTO[]> response = restClient.get()
                    .uri(url)
                    // Uncomment and modify for authentication headers if needed:
                    //.header("Authorization", "Bearer " + apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(FacilityResponseDTO[].class);

            FacilityResponseDTO[] body = response.getBody();
            if (body != null) {
                return Arrays.asList(body);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching facilities from OpenEMR: {}", e.getMessage(), e);
            // You may want to throw a custom exception or return a partial error object.
            return Collections.emptyList();
        }
    }

    /**
     * Create a new facility by POSTing to OpenEMR API.
     */
    public FacilityResponseDTO createFacility(FacilityRequestDTO request) {
        String url = openEmrApiUrl + "/facility";
        try {
            ResponseEntity<FacilityResponseDTO> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    // Uncomment and modify for authentication headers if needed:
                    //.header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .toEntity(FacilityResponseDTO.class);

            FacilityResponseDTO body = response.getBody();
            if (body != null) {
                return body;
            }
            // In case OpenEMR does not return body, echo back the request as a fallback
            FacilityResponseDTO fallback = new FacilityResponseDTO();
            org.springframework.beans.BeanUtils.copyProperties(request, fallback);
            return fallback;
        } catch (Exception e) {
            log.error("Error creating facility in OpenEMR: {}", e.getMessage(), e);
            // You may want to throw a custom exception or return a partial error object.
            FacilityResponseDTO fallback = new FacilityResponseDTO();
            org.springframework.beans.BeanUtils.copyProperties(request, fallback);
            return fallback;
        }
    }

    /**
     * Build the OpenEMR /facility API URL with non-null query params.
     */
    private String buildFacilityApiUrl(FacilityRequestDTO filter) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(openEmrApiUrl + "/facility");

        if (filter.getName() != null) builder.queryParam("name", filter.getName());
        if (filter.getFacilityNpi() != null) builder.queryParam("facility_npi", filter.getFacilityNpi());
        if (filter.getPhone() != null) builder.queryParam("phone", filter.getPhone());
        if (filter.getFax() != null) builder.queryParam("fax", filter.getFax());
        if (filter.getStreet() != null) builder.queryParam("street", filter.getStreet());
        if (filter.getCity() != null) builder.queryParam("city", filter.getCity());
        if (filter.getState() != null) builder.queryParam("state", filter.getState());
        if (filter.getPostalCode() != null) builder.queryParam("postal_code", filter.getPostalCode());
        if (filter.getCountryCode() != null) builder.queryParam("country_code", filter.getCountryCode());
        if (filter.getFederalEin() != null) builder.queryParam("federal_ein", filter.getFederalEin());
        if (filter.getWebsite() != null) builder.queryParam("website", filter.getWebsite());
        if (filter.getEmail() != null) builder.queryParam("email", filter.getEmail());
        if (filter.getDomainIdentifier() != null) builder.queryParam("domain_identifier", filter.getDomainIdentifier());
        if (filter.getFacilityTaxonomy() != null) builder.queryParam("facility_taxonomy", filter.getFacilityTaxonomy());
        if (filter.getFacilityCode() != null) builder.queryParam("facility_code", filter.getFacilityCode());
        if (filter.getBillingLocation() != null) builder.queryParam("billing_location", filter.getBillingLocation());
        if (filter.getAcceptsAssignment() != null) builder.queryParam("accepts_assignment", filter.getAcceptsAssignment());
        if (filter.getOid() != null) builder.queryParam("oid", filter.getOid());
        if (filter.getServiceLocation() != null) builder.queryParam("service_location", filter.getServiceLocation());

        return builder.build().toUriString();
    }
}
