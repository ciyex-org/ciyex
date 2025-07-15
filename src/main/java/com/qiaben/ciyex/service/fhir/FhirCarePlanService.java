package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirCarePlanResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirCarePlanService {

    private final OpenEmrFhirProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenEmrAuthService openEmrAuthService;

    // Method to fetch a list of CarePlan resources
    public List<FhirCarePlanResponseDTO> getCarePlans(Map<String, String> queryParams) {
        try {
            String baseUrl = properties.getBaseUrl() + "/fhir/CarePlan";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            String response = restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            return parseCarePlanListResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method to fetch a single CarePlan by UUID
    public FhirCarePlanResponseDTO getCarePlan(String uuid) {
        try {
            String url = properties.getBaseUrl() + "/fhir/CarePlan/" + uuid;

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Parsing a single CarePlanResponseDTO from the response
    private FhirCarePlanResponseDTO parseResponse(String response) {
        try {
            return objectMapper.readValue(response, FhirCarePlanResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CarePlan response", e);
        }
    }

    // Parsing a list of CarePlanResponseDTOs from the response
    private List<FhirCarePlanResponseDTO> parseCarePlanListResponse(String response) {
        try {
            return objectMapper.readValue(
                    response,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FhirCarePlanResponseDTO.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CarePlan list response", e);
        }
    }
}