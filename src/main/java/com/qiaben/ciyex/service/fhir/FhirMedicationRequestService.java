package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirMedicationRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirMedicationRequestService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    // Method to fetch all MedicationRequest resources with query parameters
    public FhirMedicationRequestDTO getMedicationRequests(Map<String, String> queryParams) {
        try {
            String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/MedicationRequest";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) {
                    builder.queryParam(key, value);
                }
            });

            return restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirMedicationRequestDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Method to fetch a single MedicationRequest by UUID
    public FhirMedicationRequestDTO getMedicationRequestByUuid(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/MedicationRequest/" + uuid;

            return restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirMedicationRequestDTO.class);
        }catch (Exception e) {
            throw new RuntimeException("Failed to fetch MedicationRequest by UUID", e);
        }
    }
}

