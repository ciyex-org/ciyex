package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirValueSetSearchParamsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirValueSetService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public ResponseEntity<Object> getValueSets(FhirValueSetSearchParamsDto params) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/ValueSet";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

            Map<String, String> paramMap = new HashMap<>();
            if (params.get_id() != null) paramMap.put("_id", params.get_id());
            if (params.get_lastUpdated() != null) paramMap.put("_lastUpdated", params.get_lastUpdated());
            paramMap.forEach(builder::queryParam);

            Object body = restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            return ResponseEntity.ok(body);
        }catch (Exception e) {
            throw new RuntimeException("Failed to fetch ValueSets", e);
        }
    }

    public ResponseEntity<Object> getValueSetById(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/ValueSet/" + uuid;

            Object body = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
