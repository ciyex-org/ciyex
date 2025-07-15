package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.dto.fhir.FhirBinaryResponseDTO;
import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class FhirBinaryService {

    private final RestClient restClient;
    private final OpenEmrFhirProperties openEmrFhirProperties;

    private static final String BINARY_URL = "/fhir/Binary/{id}";
    private final OpenEmrAuthService openEmrAuthService;

    public FhirBinaryResponseDTO getBinaryDocument(String id) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + BINARY_URL.replace("{id}", id);

            return restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirBinaryResponseDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
