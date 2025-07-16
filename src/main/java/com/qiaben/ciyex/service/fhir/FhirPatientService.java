package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.fhir.*;
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

    public  ApiResponse<FhirPaymentDTO> createPayment(FhirPaymentDTO payment) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Payment";

            FhirPaymentDTO createdPayment = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payment)
                    .retrieve()
                    .body(FhirPaymentDTO.class);

            return ApiResponse.<FhirPaymentDTO>builder()
                    .success(true)
                    .message("Payment recorded successfully!")
                    .data(createdPayment)
                    .build();
        } catch (Exception e) {
            return ApiResponse.<FhirPaymentDTO>builder()
                    .success(false)
                    .message("Failed to record payment: " + e.getMessage())
                    .build();
        }
    }

    // New method to create a Patient Bill
    public ApiResponse<FhirPatientBillDTO> createPatientBill(FhirPatientBillDTO bill) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/PatientBill";

            FhirPatientBillDTO createdBill = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bill)
                    .retrieve()
                    .body(FhirPatientBillDTO.class);

            return ApiResponse.<FhirPatientBillDTO>builder()
                    .success(true)
                    .message("Bill created successfully!")
                    .data(createdBill)
                    .build();
        } catch (Exception e) {
            return ApiResponse.<FhirPatientBillDTO>builder()
                    .success(false)
                    .message("Failed to create bill: " + e.getMessage())
                    .build();
        }
    }

    // New method to save Vital Signs
    public ApiResponse<FhirVitalSignsDTO> saveVitalSigns(FhirVitalSignsDTO vitals) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/VitalSigns";

            FhirVitalSignsDTO savedVitals = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vitals)
                    .retrieve()
                    .body(FhirVitalSignsDTO.class);

            return ApiResponse.<FhirVitalSignsDTO>builder()
                    .success(true)
                    .message("Vital signs saved successfully!")
                    .data(savedVitals)
                    .build();
        } catch (Exception e) {
            return ApiResponse.<FhirVitalSignsDTO>builder()
                    .success(false)
                    .message("Failed to save vital signs: " + e.getMessage())
                    .build();
        }
    }

}
