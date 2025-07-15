package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class FhirMetadataService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public ResponseEntity<Object> getMetadata() {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/metadata";

            Object body = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            return ResponseEntity.ok(body);
        }catch (Exception e) {
            throw new RuntimeException("Failed to fetch FHIR metadata", e);
        }
    }

    public ResponseEntity<Object> getSmartConfiguration() {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/.well-known/smart-configuration";

            Object body = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
