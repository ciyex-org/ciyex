package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service

public class FhirExportService {

    private final OpenEmrFhirProperties OpenEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public FhirExportService(OpenEmrFhirProperties OpenEmrFhirProperties) {
        this.OpenEmrFhirProperties = OpenEmrFhirProperties;
    }

    public ResponseEntity<Object> getBulkExportData() {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/$export";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }
}