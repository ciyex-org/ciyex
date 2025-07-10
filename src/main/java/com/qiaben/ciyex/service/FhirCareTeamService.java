package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.FhirProperties;
import com.qiaben.ciyex.dto.FhirCareTeamDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirCareTeamService {

    private final FhirProperties openEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    // Method to fetch all CareTeam resources based on query parameters
    public List<FhirCareTeamDTO> getCareTeams(Map<String, String> queryParams) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/CareTeam";
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
        ResponseEntity<List> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                List.class
        );

        return response.getBody();
    }

    // Method to fetch a single CareTeam by UUID
    public FhirCareTeamDTO getCareTeamByUuid(String uuid) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/CareTeam/" + uuid;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<FhirCareTeamDTO> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                entity,
                FhirCareTeamDTO.class
        );

        return response.getBody();
    }
}

