package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.FhirPatientDto;
import com.qiaben.ciyex.dto.FhirPatientListResponseDto;
import com.qiaben.ciyex.dto.FhirPatientSingleResponseDto;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service

public class FhirPatientService {
    private final OpenEmrFhirProperties OpenEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public FhirPatientService(OpenEmrFhirProperties OpenEmrFhirProperties) {
        this.OpenEmrFhirProperties = OpenEmrFhirProperties;
    }

    public FhirPatientListResponseDto getPatients(Map<String, String> queryParams) {
        String baseUrl = OpenEmrFhirProperties.getBaseUrl() + "/fhir/Patient";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        queryParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) builder.queryParam(key, value);
        });

        HttpHeaders headers = getAuthHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<FhirPatientListResponseDto> response = restTemplate.exchange(
                builder.toUriString(), HttpMethod.GET, entity, FhirPatientListResponseDto.class
        );
        return response.getBody();
    }

    public FhirPatientSingleResponseDto getPatientByUuid(String uuid) {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/Patient/" + uuid;
        HttpHeaders headers = getAuthHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<FhirPatientSingleResponseDto> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, FhirPatientSingleResponseDto.class
        );
        return response.getBody();
    }

    public FhirPatientSingleResponseDto createPatient(FhirPatientDto patientResource) {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/Patient";
        HttpHeaders headers = getAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<FhirPatientDto> entity = new HttpEntity<>(patientResource, headers);

        ResponseEntity<FhirPatientSingleResponseDto> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, FhirPatientSingleResponseDto.class
        );
        return response.getBody();
    }

    public FhirPatientSingleResponseDto updatePatient(String uuid, FhirPatientDto patientResource) {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/Patient/" + uuid;
        HttpHeaders headers = getAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<FhirPatientDto> entity = new HttpEntity<>(patientResource, headers);

        ResponseEntity<FhirPatientSingleResponseDto> response = restTemplate.exchange(
                url, HttpMethod.PUT, entity, FhirPatientSingleResponseDto.class
        );
        return response.getBody();
    }

    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
