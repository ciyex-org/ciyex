package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirMedicationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class FhirMedicationService {

    private final OpenEmrFhirProperties properties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public FhirMedicationDto getMedications(String id, String lastUpdated) {
        try{
        var urlBuilder = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/fhir/Medication");
        if (id != null) urlBuilder.queryParam("_id", id);
        if (lastUpdated != null) urlBuilder.queryParam("_lastUpdated", lastUpdated);

        return restClient.get()
                .uri(urlBuilder.toUriString())
                .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                .retrieve()
                .body(FhirMedicationDto.class);
    }catch(Exception e){
        e.printStackTrace();}
            return null;
    }

    public FhirMedicationDto getMedicationByUuid(String uuid) {
        try {
            String url = properties.getBaseUrl() + "/fhir/Medication/" + uuid;
            return restClient.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .retrieve()
                    .body(FhirMedicationDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
