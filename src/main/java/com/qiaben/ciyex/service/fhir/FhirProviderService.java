package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirProviderReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class FhirProviderService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestTemplate restTemplate;

    /**
     * Creates a provider (Practitioner) in OpenEMR.
     *
     * @param providerDTO Provider data in FHIR format
     * @param token Authorization token for OpenEMR
     * @return FhirProviderReportDTO
     */
    public FhirProviderReportDTO createProvider(FhirProviderReportDTO providerDTO, String token) {
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Practitioner";

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);

        // Wrap the FhirProviderReportDTO in HttpEntity
        HttpEntity<FhirProviderReportDTO> entity = new HttpEntity<>(providerDTO, headers);

        // Perform the HTTP POST request with the entity
        ResponseEntity<FhirProviderReportDTO> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, FhirProviderReportDTO.class
        );

        return response.getBody();  // Return the response body (created provider)
    }
}
