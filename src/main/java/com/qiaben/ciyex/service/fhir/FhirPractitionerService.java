package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirPractitionerRequestDto;
import com.qiaben.ciyex.dto.fhir.FhirPractitionerSearchParamsDto;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service

public class FhirPractitionerService {
    private final OpenEmrFhirProperties OpenEmrFhirProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public FhirPractitionerService(OpenEmrFhirProperties OpenEmrFhirProperties) {
        this.OpenEmrFhirProperties = OpenEmrFhirProperties;
    }

    public ResponseEntity<Object> getPractitioners(FhirPractitionerSearchParamsDto params) {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/Practitioner";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        Map<String, String> paramMap = new HashMap<>();
        if (params.get_id() != null) paramMap.put("_id", params.get_id());
        if (params.get_lastUpdated() != null) paramMap.put("_lastUpdated", params.get_lastUpdated());
        if (params.getName() != null) paramMap.put("name", params.getName());
        if (params.getActive() != null) paramMap.put("active", params.getActive());
        if (params.getAddress() != null) paramMap.put("address", params.getAddress());
        if (params.getAddressCity() != null) paramMap.put("address-city", params.getAddressCity());
        if (params.getAddressPostalcode() != null) paramMap.put("address-postalcode", params.getAddressPostalcode());
        if (params.getAddressState() != null) paramMap.put("address-state", params.getAddressState());
        if (params.getEmail() != null) paramMap.put("email", params.getEmail());
        if (params.getFamily() != null) paramMap.put("family", params.getFamily());
        if (params.getGiven() != null) paramMap.put("given", params.getGiven());
        if (params.getPhone() != null) paramMap.put("phone", params.getPhone());
        if (params.getTelecom() != null) paramMap.put("telecom", params.getTelecom());
        paramMap.forEach(builder::queryParam);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(builder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }

    public ResponseEntity<Object> getPractitionerById(String uuid) {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/Practitioner/" + uuid;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Object.class);
    }

    public ResponseEntity<Object> createPractitioner(FhirPractitionerRequestDto dto) {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/Practitioner";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(url, new HttpEntity<>(dto, headers), Object.class);
    }

    public ResponseEntity<Object> updatePractitioner(String uuid, FhirPractitionerRequestDto dto) {
        String url = OpenEmrFhirProperties.getBaseUrl() + "/fhir/Practitioner/" + uuid;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(OpenEmrFhirProperties.getToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(dto, headers), Object.class);
    }
}