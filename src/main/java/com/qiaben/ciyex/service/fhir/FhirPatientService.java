package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.FhirPatientDto;
import com.qiaben.ciyex.dto.fhir.FhirPatientListResponseDto;
import com.qiaben.ciyex.dto.fhir.FhirPatientSingleResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirPatientService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    public FhirPatientListResponseDto getPatients(Map<String, String> queryParams) {
        try {
            String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/Patient";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) builder.queryParam(key, value);
            });

            return restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirPatientListResponseDto.class);
        }catch (Exception e) {
            throw new RuntimeException("Failed to fetch patients", e);
        }
    }

    public FhirPatientSingleResponseDto getPatientByUuid(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Patient/" + uuid;

            return restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirPatientSingleResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FhirPatientSingleResponseDto createPatient(FhirPatientDto patientResource) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Patient";

            return restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patientResource)
                    .retrieve()
                    .body(FhirPatientSingleResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FhirPatientSingleResponseDto createPatient(FhirPatientDto patientResource, String token) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Patient";

            return restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patientResource)
                    .retrieve()
                    .body(FhirPatientSingleResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FhirPatientSingleResponseDto updatePatient(String uuid, FhirPatientDto patientResource) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Patient/" + uuid;

            return restClient
                    .method(HttpMethod.PUT)
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patientResource)
                    .retrieve()
                    .body(FhirPatientSingleResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
