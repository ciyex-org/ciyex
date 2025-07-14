package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service

public class FhirBulkDataStatusService {

    private final OpenEmrFhirProperties OpenEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public FhirBulkDataStatusService(OpenEmrFhirProperties OpenEmrFhirProperties) {
        this.OpenEmrFhirProperties = OpenEmrFhirProperties;
    }

    public ResponseEntity<Object> getBulkDataStatus() {
        String url = OpenEmrFhirProperties.getBaseUrl()+ "/fhir/$bulkdata-status";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }

    public ResponseEntity<Object> deleteBulkDataStatus() {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/$bulkdata-status";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Object.class);
    }
}