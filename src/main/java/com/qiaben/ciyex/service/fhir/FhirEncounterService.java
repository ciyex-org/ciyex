package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirEncounterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirEncounterService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    // Fetch all encounters for the patient
    public List<FhirEncounterDTO> getEncounters(Map<String, String> queryParams) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/portal/patient/encounter";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        queryParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                builder.queryParam(key, value);
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<List> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                List.class
        );

        return response.getBody();
    }

    // Fetch a specific encounter by UUID
    public FhirEncounterDTO getEncounterByUuid(String euuid) {
        String baseUrl = openEmrFhirProperties.getBaseUrl() + "/portal/patient/encounter/" + euuid;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<FhirEncounterDTO> response = restTemplate.exchange(
                baseUrl,
                HttpMethod.GET,
                entity,
                FhirEncounterDTO.class
        );

        return response.getBody();
    }
}

