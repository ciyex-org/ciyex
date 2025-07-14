package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class FhirBulkDataStatusService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public ResponseEntity<Object> getBulkDataStatus() {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/$bulkdata-status";

            Object response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<Object> deleteBulkDataStatus() {
        try{
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/$bulkdata-status";

        Object response = restClient
                .delete()
                .uri(url)
                .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Object.class);

        return ResponseEntity.ok(response);
    }catch (Exception e) {
        throw new RuntimeException("Failed to delete bulk data status", e);}
    }
}
