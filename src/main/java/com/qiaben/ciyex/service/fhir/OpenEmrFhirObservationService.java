package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.ObservationResponseDTO;
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
public class OpenEmrFhirObservationService {

    private final OpenEmrFhirProperties openEmrProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public ObservationResponseDTO getObservations(Map<String, String> queryParams) {
        try{
        String baseUrl = openEmrProperties.getBaseUrl() + "/Observation";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        queryParams.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                builder.queryParam(k, v);
            }
        });

        return restClient
                .get()
                .uri(builder.build(true).toUri())
                .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken()
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(ObservationResponseDTO.class);
    }catch (Exception e){
        e.printStackTrace();
        throw new RuntimeException("Failed to fetch observations: " + e.getMessage(), e);
        }
    }
    public Map<String, Object> getObservationByUuid(String uuid) {
        try {
            String url = openEmrProperties.getBaseUrl() + "/Observation/" + uuid;

            return restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch observation by UUID: " + e.getMessage(), e);
        }
    }
}
