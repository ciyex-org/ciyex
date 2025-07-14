package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.dto.fhir.ImmunizationResponseDTO;
import com.qiaben.ciyex.config.OpenEmrFhirProperties;
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
public class OpenEmrFhirImmunizationService {

    private final OpenEmrFhirProperties openEmrProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final OpenEmrAuthService openEmrAuthService;

    // Method to fetch a list of immunizations, accepts query parameters for filtering
    public List<ImmunizationResponseDTO> getImmunizations(Map<String, String> queryParams) {
        try {
            String baseUrl = openEmrProperties.getBaseUrl() + "/fhir/Immunization";
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

            return parseImmunizationListResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method to fetch a single immunization by UUID
    public ImmunizationResponseDTO getImmunization(String uuid) {
        try {
            String url = openEmrProperties.getBaseUrl() + "/fhir/Immunization/" + uuid;

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            return parseResponse(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Immunization by UUID", e);
        }
    }

    // Parsing a single ImmunizationDTO from the response
    private ImmunizationResponseDTO parseResponse(String response) {
        try {
            return objectMapper.readValue(response, ImmunizationResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Immunization response", e);
        }
    }

    // Parsing a list of ImmunizationDTOs from the response
    private List<ImmunizationResponseDTO> parseImmunizationListResponse(String response) {
        try {
            return objectMapper.readValue(
                    response,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ImmunizationResponseDTO.class)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Immunization list response", e);
        }
    }
}
