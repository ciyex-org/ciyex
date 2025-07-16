package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.fhir.FhirDiagnosticReportDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FhirDiagnosticReportService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;

    // Method to fetch all DiagnosticReports based on query parameters
    public FhirDiagnosticReportDTO getDiagnosticReports(Map<String, String> queryParams) {
        try {
            String baseUrl = openEmrFhirProperties.getBaseUrl() + "/fhir/DiagnosticReport";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((k, v) -> {
                if (v != null && !v.isEmpty()) {
                    builder.queryParam(k, v);
                }
            });

            return restClient
                    .get()
                    .uri(builder.build(true).toUri())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirDiagnosticReportDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // Method to fetch a single DiagnosticReport by UUID
    public FhirDiagnosticReportDTO getDiagnosticReportByUuid(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/DiagnosticReport/" + uuid;

            return restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(FhirDiagnosticReportDTO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public ApiResponse<FhirDiagnosticReportDTO> createDiagnosticReport(FhirDiagnosticReportDTO report) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/DiagnosticReport";

            FhirDiagnosticReportDTO createdReport = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(report)
                    .retrieve()
                    .body(FhirDiagnosticReportDTO.class);

            return ApiResponse.<FhirDiagnosticReportDTO>builder()
                    .success(true)
                    .message("Diagnostic report created successfully!")
                    .data(createdReport)
                    .build();
        } catch (Exception e) {
            return ApiResponse.<FhirDiagnosticReportDTO>builder()
                    .success(false)
                    .message("Failed to create diagnostic report: " + e.getMessage())
                    .build();
        }
    }
}
