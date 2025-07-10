package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.FhirProperties;
import com.qiaben.ciyex.dto.AllergyIntoleranceResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirAllergyIntoleranceService {

    private final FhirProperties Properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();  // Jackson ObjectMapper to parse JSON

    // Method to fetch a list of AllergyIntolerance resources
    public List<AllergyIntoleranceResponseDTO> getAllergyIntolerances(Map<String, String> queryParams) {
        String baseUrl = Properties.getBaseUrl() + "/fhir/AllergyIntolerance";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        queryParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                builder.queryParam(key, value);
            }
        });

        String response = restTemplate.getForObject(builder.toUriString(), String.class);
        // Parse and return the list of AllergyIntoleranceResponseDTO
        return parseAllergyIntoleranceListResponse(response);
    }

    // Method to fetch a single AllergyIntolerance by UUID
    public AllergyIntoleranceResponseDTO getAllergyIntolerance(String uuid) {
        String baseUrl = Properties.getBaseUrl() + "/fhir/AllergyIntolerance/" + uuid;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        String response = restTemplate.getForObject(builder.toUriString(), String.class);

        // Parse and return the AllergyIntoleranceResponseDTO
        return parseResponse(response);
    }

    // Parsing a single AllergyIntoleranceResponseDTO from the response
    private AllergyIntoleranceResponseDTO parseResponse(String response) {
        try {
            // Deserializing JSON response into AllergyIntoleranceResponseDTO
            return objectMapper.readValue(response, AllergyIntoleranceResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AllergyIntolerance response", e);
        }
    }

    // Parsing a list of AllergyIntoleranceResponseDTOs from the response
    private List<AllergyIntoleranceResponseDTO> parseAllergyIntoleranceListResponse(String response) {
        try {
            // Deserializing JSON response into a list of AllergyIntoleranceResponseDTO
            return objectMapper.readValue(response, objectMapper.getTypeFactory().constructCollectionType(List.class, AllergyIntoleranceResponseDTO.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AllergyIntolerance list response", e);
        }
    }
}