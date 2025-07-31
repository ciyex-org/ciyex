package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;


import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirPatientService {

    private final RestClient restClient;
    private final FhirAuthService fhirAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    // ----------- PATIENT METHODS -----------

    public Bundle getPatients(Map<String, String> queryParams) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(fhirConfig.getApiUrl() + "/Patient");
            queryParams.forEach((key, value) -> {
                if (value != null && !value.isEmpty()) builder.queryParam(key, value);
            });

            url = builder.build(true).toUriString();
            log.info("[FhirPatientService] Fetching patients from URL: {}", url);

            String responseBody = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPatientService] Patients response: {}", responseBody);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Bundle.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirPatientService] Failed to fetch patients from URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch patients", e);
        }
    }

    public Patient getPatientByUuid(String uuid) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Patient/" + uuid;

            log.info("[FhirPatientService] Fetching patient by UUID {} from URL: {}", uuid, url);

            String responseBody = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPatientService] Patient response: {}", responseBody);

            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Patient.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirPatientService] Failed to fetch patient UUID {} from URL: {}. Error: {}", uuid, url, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Patient createPatient(Patient patientResource) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Patient";

            IParser parser = fhirContext.newJsonParser();
            String patientJson = parser.encodeResourceToString(patientResource);

            log.info("[FhirPatientService] Creating patient at URL: {}", url);
            log.debug("[FhirPatientService] Patient payload: {}", patientJson);

            String responseBody = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patientJson)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPatientService] Patient creation response: {}", responseBody);

            return parser.parseResource(Patient.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirPatientService] Failed to create patient at URL: {}. Error: {}", url, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Patient updatePatient(String uuid, Patient patientResource) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Patient/" + uuid;

            IParser parser = fhirContext.newJsonParser();
            String patientJson = parser.encodeResourceToString(patientResource);

            log.info("[FhirPatientService] Updating patient UUID {} at URL: {}", uuid, url);
            log.debug("[FhirPatientService] Patient update payload: {}", patientJson);

            String responseBody = restClient
                    .method(HttpMethod.PUT)
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patientJson)
                    .retrieve()
                    .body(String.class);

            log.debug("[FhirPatientService] Patient update response: {}", responseBody);

            return parser.parseResource(Patient.class, responseBody);
        } catch (Exception e) {
            log.error("[FhirPatientService] Failed to update patient UUID {} at URL: {}. Error: {}", uuid, url, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // ----------- PAYMENT METHODS (FHIR PaymentReconciliation) -----------

    public ApiResponse<PaymentReconciliation> createPayment(PaymentReconciliation paymentResource) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/PaymentReconciliation";

            IParser parser = fhirContext.newJsonParser();
            String paymentJson = parser.encodeResourceToString(paymentResource);

            log.info("[FhirPatientService] Creating payment at URL: {}", url);
            log.debug("[FhirPatientService] Payment payload: {}", paymentJson);

            String responseBody = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(paymentJson)
                    .retrieve()
                    .body(String.class);

            PaymentReconciliation createdPayment = parser.parseResource(PaymentReconciliation.class, responseBody);

            log.debug("[FhirPatientService] Payment creation response: {}", responseBody);

            return ApiResponse.<PaymentReconciliation>builder()
                    .success(true)
                    .message("Payment recorded successfully!")
                    .data(createdPayment)
                    .build();
        } catch (Exception e) {
            log.error("[FhirPatientService] Failed to create payment at URL: {}. Error: {}", url, e.getMessage(), e);
            return ApiResponse.<PaymentReconciliation>builder()
                    .success(false)
                    .message("Failed to record payment: " + e.getMessage())
                    .build();
        }
    }


    // ----------- BILL METHODS (FHIR Invoice) -----------

    public ApiResponse<Invoice> createPatientBill(Invoice billResource) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Invoice";

            IParser parser = fhirContext.newJsonParser();
            String billJson = parser.encodeResourceToString(billResource);

            log.info("[FhirPatientService] Creating patient bill at URL: {}", url);
            log.debug("[FhirPatientService] Bill payload: {}", billJson);

            String responseBody = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(billJson)
                    .retrieve()
                    .body(String.class);

            Invoice createdBill = parser.parseResource(Invoice.class, responseBody);

            log.debug("[FhirPatientService] Patient bill creation response: {}", responseBody);

            return ApiResponse.<Invoice>builder()
                    .success(true)
                    .message("Bill created successfully!")
                    .data(createdBill)
                    .build();
        } catch (Exception e) {
            log.error("[FhirPatientService] Failed to create patient bill at URL: {}. Error: {}", url, e.getMessage(), e);
            return ApiResponse.<Invoice>builder()
                    .success(false)
                    .message("Failed to create bill: " + e.getMessage())
                    .build();
        }
    }

    // ----------- VITAL SIGNS METHODS (FHIR Observation) -----------

    public ApiResponse<Observation> saveVitalSigns(Observation vitalsResource) {
        String url = null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            url = fhirConfig.getApiUrl() + "/Observation";

            IParser parser = fhirContext.newJsonParser();
            String vitalsJson = parser.encodeResourceToString(vitalsResource);

            log.info("[FhirPatientService] Saving vital signs at URL: {}", url);
            log.debug("[FhirPatientService] Vital signs payload: {}", vitalsJson);

            String responseBody = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + fhirAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vitalsJson)
                    .retrieve()
                    .body(String.class);

            Observation savedVitals = parser.parseResource(Observation.class, responseBody);

            log.debug("[FhirPatientService] Vital signs save response: {}", responseBody);

            return ApiResponse.<Observation>builder()
                    .success(true)
                    .message("Vital signs saved successfully!")
                    .data(savedVitals)
                    .build();
        } catch (Exception e) {
            log.error("[FhirPatientService] Failed to save vital signs at URL: {}. Error: {}", url, e.getMessage(), e);
            return ApiResponse.<Observation>builder()
                    .success(false)
                    .message("Failed to save vital signs: " + e.getMessage())
                    .build();
        }
    }
}
