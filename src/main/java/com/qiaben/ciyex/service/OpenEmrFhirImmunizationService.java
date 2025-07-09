package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ImmunizationResponseDTO;
import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenEmrFhirImmunizationService {

    private final OpenEmrFhirProperties openEmrProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();  // Jackson ObjectMapper to parse JSON

    // Method to fetch a list of immunizations, accepts query parameters for filtering
    public List<ImmunizationResponseDTO> getImmunizations(Map<String, String> queryParams) {
        String baseUrl = openEmrProperties.getBaseUrl() + "/fhir/Immunization";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        queryParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                builder.queryParam(key, value);
            }
        });

        String response = restTemplate.getForObject(builder.toUriString(), String.class);
        // Parse the response into a list of ImmunizationDTO objects
        return parseImmunizationListResponse(response);  // Using a method to parse the list of immunizations
    }

    // Method to fetch a single immunization by UUID
    public ImmunizationResponseDTO getImmunization(String uuid) {
        String baseUrl = openEmrProperties.getBaseUrl() + "/fhir/Immunization/" + uuid;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        // Making the GET request
        String response = restTemplate.getForObject(builder.toUriString(), String.class);

        // Parsing the response to a single ImmunizationDTO
        return parseResponse(response);  // Parsing single response into ImmunizationDTO
    }

    // Parsing a single ImmunizationDTO from the response
    private ImmunizationResponseDTO parseResponse(String response) {
        try {
            // Deserializing JSON response into ImmunizationDTO
            return objectMapper.readValue(response, ImmunizationResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Immunization response", e);
        }
    }

    // Parsing a list of ImmunizationDTOs from the response
    private List<ImmunizationResponseDTO> parseImmunizationListResponse(String response) {
        try {
            // Deserializing JSON response into a list of ImmunizationDTO
            // Assuming the response has a "bundle" format with "entry" as the list
            return objectMapper.readValue(response, objectMapper.getTypeFactory().constructCollectionType(List.class, ImmunizationResponseDTO.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Immunization list response", e);
        }
    }
}
