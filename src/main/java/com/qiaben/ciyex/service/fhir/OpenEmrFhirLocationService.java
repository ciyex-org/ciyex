package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.LocationResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenEmrFhirLocationService {

    private final OpenEmrFhirProperties openEmrProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();  // Jackson ObjectMapper to parse JSON

    // Method to fetch a list of locations, accepts query parameters for filtering
    public List<LocationResponseDTO> getLocations(Map<String, String> queryParams) {
        String baseUrl = openEmrProperties.getBaseUrl() + "/fhir/Location";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        queryParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                builder.queryParam(key, value);
            }
        });

        String response = restTemplate.getForObject(builder.toUriString(), String.class);
        // Parse the response into a list of LocationResponseDTO objects
        return parseLocationListResponse(response);  // Using a method to parse the list of locations
    }

    // Method to fetch a single location by UUID
    public LocationResponseDTO getLocation(String uuid) {
        String baseUrl = openEmrProperties.getBaseUrl() + "/fhir/Location/" + uuid;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        // Making the GET request to fetch the location
        String response = restTemplate.getForObject(builder.toUriString(), String.class);

        // If the response is null or empty, return null (this could be adapted to throw an exception for 404)
        if (response == null || response.isEmpty()) {
            return null;
        }

        // Parse and return the LocationResponseDTO
        return parseResponse(response);
    }

    // Parsing a single LocationResponseDTO from the response
    private LocationResponseDTO parseResponse(String response) {
        try {
            // Deserializing JSON response into LocationResponseDTO
            return objectMapper.readValue(response, LocationResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Location response", e);
        }
    }

    // Parsing a list of LocationResponseDTOs from the response
    private List<LocationResponseDTO> parseLocationListResponse(String response) {
        try {
            // Deserializing JSON response into a list of LocationResponseDTO
            // Assuming the response has a "bundle" format with "entry" as the list
            return objectMapper.readValue(response, objectMapper.getTypeFactory().constructCollectionType(List.class, LocationResponseDTO.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Location list response", e);
        }
    }
}
