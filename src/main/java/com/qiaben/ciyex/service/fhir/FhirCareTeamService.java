package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirCareTeamDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirCareTeamService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    // Method to fetch all CareTeam resources based on query parameters
    public List<FhirCareTeamDTO> getCareTeams(Map<String, String> queryParams) {
        try{
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/CareTeam";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        queryParams.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                builder.queryParam(k, v);
            }
        });

        return restClient
                .get()
                .uri(builder.build(true).toUri())
                .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<FhirCareTeamDTO>>() {});
    }catch (Exception e) {
            throw new RuntimeException("Failed to fetch CareTeams", e);
        }
    }
    // Method to fetch a single CareTeam by UUID
    public FhirCareTeamDTO getCareTeamByUuid(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/CareTeam/" + uuid;

            return restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirCareTeamDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch CareTeam by UUID", e);
        }
    }
}

