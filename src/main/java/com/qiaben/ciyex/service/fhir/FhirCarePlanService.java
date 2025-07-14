package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirCarePlanResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirCarePlanService {

    private final OpenEmrFhirProperties Properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();  // Jackson ObjectMapper to parse JSON

    // Method to fetch a list of CarePlan resources
    public List<FhirCarePlanResponseDTO> getCarePlans(Map<String, String> queryParams) {
        String baseUrl = Properties.getBaseUrl() + "/fhir/CarePlan";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        queryParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                builder.queryParam(key, value);
            }
        });

        String response = restTemplate.getForObject(builder.toUriString(), String.class);
        // Parse and return the list of CarePlanResponseDTO
        return parseCarePlanListResponse(response);
    }

    // Method to fetch a single CarePlan by UUID
    public FhirCarePlanResponseDTO getCarePlan(String uuid) {
        String baseUrl = Properties.getBaseUrl() + "/fhir/CarePlan/" + uuid;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        String response = restTemplate.getForObject(builder.toUriString(), String.class);

        // Parse and return the CarePlanResponseDTO
        return parseResponse(response);
    }

    // Parsing a single CarePlanResponseDTO from the response
    private FhirCarePlanResponseDTO parseResponse(String response) {
        try {
            // Deserializing JSON response into CarePlanResponseDTO
            return objectMapper.readValue(response, FhirCarePlanResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CarePlan response", e);
        }
    }

    // Parsing a list of CarePlanResponseDTOs from the response
    private List<FhirCarePlanResponseDTO> parseCarePlanListResponse(String response) {
        try {
            // Deserializing JSON response into a list of CarePlanResponseDTO
            return objectMapper.readValue(response, objectMapper.getTypeFactory().constructCollectionType(List.class, FhirCarePlanResponseDTO.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CarePlan list response", e);
        }
    }
}