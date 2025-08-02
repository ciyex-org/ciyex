package com.qiaben.ciyex.storage.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirDiagnosticReportService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // Fetch all DiagnosticReports based on query parameters
    public Bundle getDiagnosticReports(Map<String, String> queryParams) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String baseUrl = fhirConfig.getApiUrl() + "/DiagnosticReport";
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);

            queryParams.forEach((k, v) -> {
                if (v != null && !v.isEmpty()) builder.queryParam(k, v);
            });
            url = builder.build(true).toUriString();

            log.info("[FhirDiagnosticReportService] Fetching DiagnosticReports for clientId={}, url={}, params={}",
                    fhirConfig.getClientId(), url, queryParams);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirDiagnosticReportService] DiagnosticReport bundle response: {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);

            log.info("[FhirDiagnosticReportService] Parsed {} DiagnosticReport entries from bundle.", bundle.getEntry().size());
            return bundle;
        } catch (Exception e) {
            log.error("[FhirDiagnosticReportService] Error fetching DiagnosticReports (clientId={}, url={}, params={})",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, queryParams, e);
            throw new RuntimeException("Failed to fetch DiagnosticReports", e);
        }
    }

    // Fetch a single DiagnosticReport by UUID
    public DiagnosticReport getDiagnosticReportByUuid(String uuid) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/DiagnosticReport/" + uuid;

            log.info("[FhirDiagnosticReportService] Fetching DiagnosticReport by UUID for clientId={}, url={}, uuid={}",
                    fhirConfig.getClientId(), url, uuid);

            String response = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirDiagnosticReportService] FHIR DiagnosticReport response: {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            IParser parser = fhirContext.newJsonParser();
            DiagnosticReport report = parser.parseResource(DiagnosticReport.class, response);

            log.info("[FhirDiagnosticReportService] Successfully parsed DiagnosticReport for UUID={}", uuid);
            return report;
        } catch (Exception e) {
            log.error("[FhirDiagnosticReportService] Error fetching DiagnosticReport by UUID (clientId={}, url={}, uuid={})",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, uuid, e);
            throw new RuntimeException("Failed to fetch DiagnosticReport by UUID", e);
        }
    }

    // Create DiagnosticReport in FHIR
    public ApiResponse<DiagnosticReport> createDiagnosticReport(DiagnosticReport report) {
        FhirConfig fhirConfig = null;
        String url = null;
        try {
            fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/DiagnosticReport";

            IParser parser = fhirContext.newJsonParser();
            String reportJson = parser.encodeResourceToString(report);

            log.info("[FhirDiagnosticReportService] Creating DiagnosticReport for clientId={}, url={}",
                    fhirConfig.getClientId(), url);

            String response = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(reportJson)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirDiagnosticReportService] FHIR DiagnosticReport create response: {}",
                    response != null ? response.substring(0, Math.min(response.length(), 400)) : "null");

            DiagnosticReport createdReport = parser.parseResource(DiagnosticReport.class, response);

            log.info("[FhirDiagnosticReportService] DiagnosticReport created successfully, id={}", createdReport.getIdElement().getIdPart());
            return ApiResponse.<DiagnosticReport>builder()
                    .success(true)
                    .message("Diagnostic report created successfully!")
                    .data(createdReport)
                    .build();
        } catch (Exception e) {
            log.error("[FhirDiagnosticReportService] Error creating DiagnosticReport (clientId={}, url={})",
                    fhirConfig != null ? fhirConfig.getClientId() : null,
                    url, e);
            return ApiResponse.<DiagnosticReport>builder()
                    .success(false)
                    .message("Failed to create diagnostic report: " + e.getMessage())
                    .build();
        }
    }
}
