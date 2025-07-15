package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.CoverageResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenEmrFhirCoverageService {

    private final RestClient restClient;
    private final OpenEmrFhirProperties properties;
    private final OpenEmrAuthService openEmrAuthService;

    public CoverageResponseDTO getCoverage(String id, String lastUpdated, String patient, String payor) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(properties.getBaseUrl() + "/fhir/Coverage")
                    .queryParamIfPresent("_id", id != null ? java.util.Optional.of(id) : java.util.Optional.empty())
                    .queryParamIfPresent("_lastUpdated", lastUpdated != null ? java.util.Optional.of(lastUpdated) : java.util.Optional.empty())
                    .queryParamIfPresent("patient", patient != null ? java.util.Optional.of(patient) : java.util.Optional.empty())
                    .queryParamIfPresent("payor", payor != null ? java.util.Optional.of(payor) : java.util.Optional.empty())
                    .toUriString();

            return restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CoverageResponseDTO.class);
        } catch (Exception e) {
            log.error("Error getting coverage", e);
            throw new RuntimeException("Failed to fetch coverage", e);
        }
    }

    public Map<String, Object> getCoverageByUuid(String uuid) {
        try {
            String url = properties.getBaseUrl() + "/fhir/Coverage/" + uuid;

            return restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("Error getting coverage by UUID", e);
            throw new RuntimeException("Failed to fetch coverage by UUID", e);
        }
    }
}
