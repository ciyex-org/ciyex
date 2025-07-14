package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.ObservationResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenEmrFhirObservationService {

    private final OpenEmrFhirProperties openEmrProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public ObservationResponseDTO getObservations(Map<String, String> queryParams) {
        String url = openEmrProperties.getBaseUrl() + "/Observation";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

        queryParams.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                builder.queryParam(key, value);
            }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ObservationResponseDTO> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                ObservationResponseDTO.class
        );

        return response.getBody();
    }

    public Map<String, Object> getObservationByUuid(String uuid) {
        String url = openEmrProperties.getBaseUrl() + "/Observation/" + uuid;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openEmrProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        return response.getBody();
    }
}
