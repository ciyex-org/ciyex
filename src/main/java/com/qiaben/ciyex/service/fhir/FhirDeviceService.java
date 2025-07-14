package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.dto.fhir.FhirDeviceDTO;
import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FhirDeviceService {

    private final RestClient restClient;
    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final OpenEmrAuthService openEmrAuthService;

    public List<FhirDeviceDTO> getDevices(String _id, String _lastUpdated, String patient) {
        try {
            String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/Device";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            if (_id != null && !_id.isEmpty()) builder.queryParam("_id", _id);
            if (_lastUpdated != null && !_lastUpdated.isEmpty()) builder.queryParam("_lastUpdated", _lastUpdated);
            if (patient != null && !patient.isEmpty()) builder.queryParam("patient", patient);

            return restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<FhirDeviceDTO>>() {
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FhirDeviceDTO getDeviceById(String uuid) {
        try{
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Device/" + uuid;

        return restClient
                .get()
                .uri(url)
                .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(FhirDeviceDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Device by UUID", e);}
    }
}
