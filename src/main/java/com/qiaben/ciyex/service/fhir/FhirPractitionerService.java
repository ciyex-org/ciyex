package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirPractitionerRequestDto;
import com.qiaben.ciyex.dto.fhir.FhirPractitionerSearchParamsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
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
public class FhirPractitionerService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public ResponseEntity<Object> getPractitioners(FhirPractitionerSearchParamsDto params) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Practitioner";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            Map<String, String> paramMap = new HashMap<>();
            if (params.get_id() != null) paramMap.put("_id", params.get_id());
            if (params.get_lastUpdated() != null) paramMap.put("_lastUpdated", params.get_lastUpdated());
            if (params.getName() != null) paramMap.put("name", params.getName());
            if (params.getActive() != null) paramMap.put("active", params.getActive());
            if (params.getAddress() != null) paramMap.put("address", params.getAddress());
            if (params.getAddressCity() != null) paramMap.put("address-city", params.getAddressCity());
            if (params.getAddressPostalcode() != null)
                paramMap.put("address-postalcode", params.getAddressPostalcode());
            if (params.getAddressState() != null) paramMap.put("address-state", params.getAddressState());
            if (params.getEmail() != null) paramMap.put("email", params.getEmail());
            if (params.getFamily() != null) paramMap.put("family", params.getFamily());
            if (params.getGiven() != null) paramMap.put("given", params.getGiven());
            if (params.getPhone() != null) paramMap.put("phone", params.getPhone());
            if (params.getTelecom() != null) paramMap.put("telecom", params.getTelecom());
            paramMap.forEach(builder::queryParam);

            Object body = restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Object.class);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<Object> getPractitionerById(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Practitioner/" + uuid;

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

    public ResponseEntity<Object> createPractitioner(FhirPractitionerRequestDto dto) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Practitioner";

            Object body = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .body(Object.class);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<Object> updatePractitioner(String uuid, FhirPractitionerRequestDto dto) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Practitioner/" + uuid;

            Object body = restClient
                    .method(HttpMethod.PUT)
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .body(Object.class);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}