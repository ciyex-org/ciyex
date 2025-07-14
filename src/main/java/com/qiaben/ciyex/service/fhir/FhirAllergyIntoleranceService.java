package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.AllergyIntoleranceResponseDTO;
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
public class FhirAllergyIntoleranceService {

    private final OpenEmrFhirProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenEmrAuthService openEmrAuthService;

    // Method to fetch a list of AllergyIntolerance resources
    public List<AllergyIntoleranceResponseDTO> getAllergyIntolerances(Map<String, String> queryParams) {
        try {
            String baseUrl = properties.getBaseUrl() + "/fhir/AllergyIntolerance";
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

            return parseAllergyIntoleranceListResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method to fetch a single AllergyIntolerance by UUID
    public AllergyIntoleranceResponseDTO getAllergyIntolerance(String uuid) {
        try {
            String url = properties.getBaseUrl() + "/fhir/AllergyIntolerance/" + uuid;

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

    // Parsing a single AllergyIntoleranceResponseDTO from the response
    private AllergyIntoleranceResponseDTO parseResponse(String response) {
        try {
            return objectMapper.readValue(response, AllergyIntoleranceResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AllergyIntolerance response", e);
        }
    }

    // Parsing a list of AllergyIntoleranceResponseDTOs from the response
    private List<AllergyIntoleranceResponseDTO> parseAllergyIntoleranceListResponse(String response) {
        try {
            return objectMapper.readValue(
                    response,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AllergyIntoleranceResponseDTO.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AllergyIntolerance list response", e);
        }
    }
}