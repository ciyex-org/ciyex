package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.GroupResponseDTO;
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
public class OpenEmrFhirGroupService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public GroupResponseDTO getGroup(Map<String, String> queryParams) {
        try {
            String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/Group";
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
                    .body(GroupResponseDTO.class);
        }catch (Exception e) {
            throw new RuntimeException("Failed to fetch groups", e);
        }
    }
    public GroupResponseDTO getGroupByUuid(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Group/" + uuid;

            return restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(GroupResponseDTO.class);
        }catch (Exception e) {
            throw new RuntimeException("Failed to fetch group by UUID", e);
        }
    }

    // Method to trigger the export operation for a specific Group by id
    public ResponseEntity<String> exportGroup(String id) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Group/" + id + "/$export";

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
