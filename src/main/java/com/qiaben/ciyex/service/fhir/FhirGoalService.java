package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirGoalDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirGoalService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    // Method to fetch all Goal resources based on query parameters
    public FhirGoalDTO getGoals(Map<String, String> queryParams) {
        try {
            String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/Goal";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            return restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirGoalDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method to fetch a single Goal by UUID
    public FhirGoalDTO getGoalByUuid(String uuid) {
        try{
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Goal/" + uuid;

        return restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(FhirGoalDTO.class);
    }catch(Exception e){
        throw new RuntimeException("Failed to fetch Goal by UUID", e);}
    }
}

