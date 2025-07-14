package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirEncounterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirEncounterService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    // Fetch all encounters for the patient
    public List<FhirEncounterDTO> getEncounters(Map<String, String> queryParams) {
        try {
            String baseUrl = openEmrFhirProperties.getBaseUrl() + "/portal/patient/encounter";
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
                    .body(new ParameterizedTypeReference<List<FhirEncounterDTO>>() {
                    });
        } catch (Exception e) {
            e.printStackTrace();
            return List.of(); // Return an empty list in case of error
        }
    }

    // Fetch a specific encounter by UUID
    public FhirEncounterDTO getEncounterByUuid(String euuid) {
        try {
            String baseUrl = openEmrFhirProperties.getBaseUrl() + "/portal/patient/encounter/" + euuid;

            return restClient
                    .get()
                    .uri(baseUrl)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirEncounterDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
