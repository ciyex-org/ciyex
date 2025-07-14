package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service

public class FhirOperationDefinitionService {

    private final OpenEmrFhirProperties OpenEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public FhirOperationDefinitionService(OpenEmrFhirProperties OpenEmrFhirProperties) {
        this.OpenEmrFhirProperties = OpenEmrFhirProperties;
    }

    public ResponseEntity<Object> getAllOperationDefinitions() {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/OperationDefinition";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }

    public ResponseEntity<Object> getOperationDefinitionByName(String operation) {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/OperationDefinition/" + operation;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }
}