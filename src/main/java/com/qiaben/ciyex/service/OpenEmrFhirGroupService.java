package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.GroupResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenEmrFhirGroupService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public GroupResponseDTO getGroup(Map<String, String> queryParams) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/Group";
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

        ResponseEntity<GroupResponseDTO> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, GroupResponseDTO.class);

        return response.getBody();
    }
    public GroupResponseDTO getGroupByUuid(String uuid) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/Group/" + uuid;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<GroupResponseDTO> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, GroupResponseDTO.class);

        return response.getBody();
    }

    // Method to trigger the export operation for a specific Group by id
    public ResponseEntity<String> exportGroup(String id) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/Group/" + id + "/$export";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, String.class);
    }
}
