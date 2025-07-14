package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FhirExportService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public ResponseEntity<Object> getBulkExportData() {
        try{
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/$export";

        Object response = restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Object.class);

        return ResponseEntity.ok(response);
    } catch (Exception e) {
            throw new RuntimeException(e);}
        }
}
