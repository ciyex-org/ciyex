package com.qiaben.ciyex.service;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.FhirPersonByIdResponseDto;
import com.qiaben.ciyex.dto.FhirPersonListResponseDto;
import com.qiaben.ciyex.dto.FhirPersonSearchRequestDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service

public class FhirPersonService {

    private final OpenEmrFhirProperties properties;

    public FhirPersonService(OpenEmrFhirProperties properties) {
        this.properties = properties;
    }

    public FhirPersonListResponseDto getPersons(FhirPersonSearchRequestDto req) {
        String baseUrl = properties.getBaseUrl() + "/fhir/Person";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("_id", req.getId());
        queryParams.put("_lastUpdated", req.getLastUpdated());

        queryParams.put("name", req.getName());
        queryParams.put("active", req.getActive());
        queryParams.put("address", req.getAddress());
        queryParams.put("address-city", req.getAddressCity());
        queryParams.put("address-postalcode", req.getAddressPostalcode());
        queryParams.put("address-state", req.getAddressState());
        queryParams.put("email", req.getEmail());
        queryParams.put("family", req.getFamily());
        queryParams.put("given", req.getGiven());
        queryParams.put("phone", req.getPhone());
        queryParams.put("telecom", req.getTelecom());

        queryParams.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                builder.queryParam(key, value);
            }
        });

        RestClient restClient = RestClient.builder().build();

        Map<String, Object> result = restClient.get()
                .uri(builder.toUriString())
                .header("Authorization", "Bearer " + properties.getToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        FhirPersonListResponseDto dto = new FhirPersonListResponseDto();
        dto.setJsonObject(result);
        return dto;
    }

    public FhirPersonByIdResponseDto getPersonById(String uuid) {
        String url = properties.getBaseUrl() + "/fhir/Person/" + uuid;

        RestClient restClient = RestClient.builder().build();

        Map<String, Object> result = restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + properties.getToken())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

        FhirPersonByIdResponseDto dto = new FhirPersonByIdResponseDto();
        dto.setData(result);
        return dto;
    }
}
