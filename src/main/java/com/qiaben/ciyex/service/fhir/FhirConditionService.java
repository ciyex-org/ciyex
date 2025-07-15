package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirConditionResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class FhirConditionService {

    private final OpenEmrFhirProperties FhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public ResponseEntity<FhirConditionResponseDTO> getConditions(

            String id, String lastUpdated, String patient
    ) {
        // Construct the URL with parameters
        try {
            String url = UriComponentsBuilder.fromHttpUrl(FhirProperties.getBaseUrl())
                    .path("/fhir/Condition")
                    .queryParam("_id", id)
                    .queryParam("_lastUpdated", lastUpdated)
                    .queryParam("patient", patient)
                    .toUriString();

            // Perform the GET request using RestClient and retrieve the response
            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .retrieve()  // Perform the request
                    .toEntity(FhirConditionResponseDTO.class);  // Map the response to ResponseEntity
        }catch (Exception e) {
            throw new RuntimeException("Failed to fetch conditions", e);
        }
    }

    public ResponseEntity<FhirConditionResponseDTO> getConditionByUuid(String uuid) {
        try {
            // Construct the URL for the /fhir/Condition/{uuid} endpoint
            String url = UriComponentsBuilder.fromHttpUrl(FhirProperties.getBaseUrl())
                    .path("/fhir/Condition/{uuid}")
                    .buildAndExpand(uuid) // Path variable replacement
                    .toUriString();

            // Perform the GET request using RestClient
            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .retrieve() // Initiate the request
                    .toEntity(FhirConditionResponseDTO.class); // Convert the response to ResponseEntity
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

