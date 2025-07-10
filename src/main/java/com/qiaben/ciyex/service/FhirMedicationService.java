package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.FhirMedicationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class FhirMedicationService {

    private final OpenEmrFhirProperties properties;
    private final RestClient restClient;

    public FhirMedicationDto getMedications(String id, String lastUpdated) {
        var urlBuilder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/fhir/Medication");
        if (id != null) urlBuilder.queryParam("_id", id);
        if (lastUpdated != null) urlBuilder.queryParam("_lastUpdated", lastUpdated);

        return restClient.get()
                .uri(urlBuilder.toUriString())
                .header("Authorization", "Bearer " + properties.getToken())
                .retrieve()
                .body(FhirMedicationDto.class);
    }

    public FhirMedicationDto getMedicationByUuid(String uuid) {
        String url = properties.getBaseUrl() + "/fhir/Medication/" + uuid;
        return restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getToken())
                .retrieve()
                .body(FhirMedicationDto.class);
    }
}
