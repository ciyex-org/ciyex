package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirValueSetSearchParamsDto;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service

public class FhirValueSetService {

    private final OpenEmrFhirProperties OpenEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public FhirValueSetService(OpenEmrFhirProperties OpenEmrFhirProperties) {
        this.OpenEmrFhirProperties = OpenEmrFhirProperties;
    }

    public ResponseEntity<Object> getValueSets(FhirValueSetSearchParamsDto params) {
        String url = OpenEmrFhirProperties.getBaseUrl()+ "/fhir/ValueSet";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        Map<String, String> paramMap = new HashMap<>();
        if (params.get_id() != null) paramMap.put("_id", params.get_id());
        if (params.get_lastUpdated() != null) paramMap.put("_lastUpdated", params.get_lastUpdated());
        paramMap.forEach(builder::queryParam);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }

    public ResponseEntity<Object> getValueSetById(String uuid) {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/ValueSet/" + uuid;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }
}