package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.FhirProperties;
import com.qiaben.ciyex.dto.FhirDiagnosticReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirDiagnosticReportService {

    private final FhirProperties openEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    // Method to fetch all DiagnosticReports based on query parameters
    public FhirDiagnosticReportDTO getDiagnosticReports(Map<String, String> queryParams) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/DiagnosticReport";
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
        ResponseEntity<FhirDiagnosticReportDTO> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                FhirDiagnosticReportDTO.class
        );

        return response.getBody();
    }

    // Method to fetch a single DiagnosticReport by UUID
    public FhirDiagnosticReportDTO getDiagnosticReportByUuid(String uuid) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/DiagnosticReport/" + uuid;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<FhirDiagnosticReportDTO> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                entity,
                FhirDiagnosticReportDTO.class
        );

        return response.getBody();
    }
}
