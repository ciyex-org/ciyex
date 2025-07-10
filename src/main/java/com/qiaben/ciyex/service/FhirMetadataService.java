package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service

public class FhirMetadataService {

    private final OpenEmrFhirProperties OpenEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public FhirMetadataService(OpenEmrFhirProperties OpenEmrFhirProperties) {
        this.OpenEmrFhirProperties = OpenEmrFhirProperties;
    }

    public ResponseEntity<Object> getMetadata() {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/metadata";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }

    public ResponseEntity<Object> getSmartConfiguration() {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/.well-known/smart-configuration";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }
}