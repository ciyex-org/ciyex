package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirGoalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirGoalService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    // Method to fetch all Goal resources based on query parameters
    public FhirGoalDTO getGoals(Map<String, String> queryParams) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/Goal";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        queryParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                builder.queryParam(key, value);
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<FhirGoalDTO> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                FhirGoalDTO.class
        );

        return response.getBody();
    }

    // Method to fetch a single Goal by UUID
    public FhirGoalDTO getGoalByUuid(String uuid) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/Goal/" + uuid;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<FhirGoalDTO> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                entity,
                FhirGoalDTO.class
        );

        return response.getBody();
    }
}

