package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.LocationResponseDTO;
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
public class OpenEmrFhirLocationService {

    private final OpenEmrFhirProperties openEmrProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenEmrAuthService openEmrAuthService;

    // Method to fetch a list of locations, accepts query parameters for filtering
    public List<LocationResponseDTO> getLocations(Map<String, String> queryParams) {
        try {
            String baseUrl = openEmrProperties.getBaseUrl() + "/fhir/Location";
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

            return parseLocationListResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method to fetch a single location by UUID
    public LocationResponseDTO getLocation(String uuid) {
        try {
            String url = openEmrProperties.getBaseUrl() + "/fhir/Location/" + uuid;

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isEmpty()) {
                return null;
            }

            return parseResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Parsing a single LocationResponseDTO from the response
    private LocationResponseDTO parseResponse(String response) {
        try {
            return objectMapper.readValue(response, LocationResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Location response", e);
        }
    }

    // Parsing a list of LocationResponseDTOs from the response
    private List<LocationResponseDTO> parseLocationListResponse(String response) {
        try {
            return objectMapper.readValue(
                    response,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LocationResponseDTO.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Location list response", e);
        }
    }
}
