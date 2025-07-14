package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirProcedureByIdResponseDto;
import com.qiaben.ciyex.dto.fhir.FhirProcedureListResponseDto;
import com.qiaben.ciyex.dto.fhir.FhirProcedureSearchRequestDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service

public class FhirProcedureService {

    private final OpenEmrFhirProperties properties;
    private final OpenEmrAuthService openEmrAuthService;

    public FhirProcedureService(OpenEmrFhirProperties properties, OpenEmrAuthService openEmrAuthService) {
        this.properties = properties;
        this.openEmrAuthService = openEmrAuthService;
    }

    public FhirProcedureListResponseDto getProcedures(FhirProcedureSearchRequestDto req) {
        try {
            String url = properties.getBaseUrl() + "/fhir/Procedure";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);

            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("_id", req.getId());
            queryParams.put("_lastUpdated", req.getLastUpdated());
            queryParams.put("patient", req.getPatient());
            queryParams.put("date", req.getDate());

            queryParams.forEach((key, value) -> {
                if (value != null && !value.isBlank()) {
                    builder.queryParam(key, value);
                }
            });

            RestClient client = RestClient.builder().build();

            Map<String, Object> response = client.get()
                    .uri(builder.toUriString())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            FhirProcedureListResponseDto dto = new FhirProcedureListResponseDto();
            dto.setJsonObject(response);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FhirProcedureByIdResponseDto getProcedureById(String uuid) {
        try {
            String url = properties.getBaseUrl() + "/fhir/Procedure/" + uuid;

            RestClient client = RestClient.builder().build();

            Map<String, Object> response = client.get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            FhirProcedureByIdResponseDto dto = new FhirProcedureByIdResponseDto();
            dto.setData(response);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
