package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirPatientService {

    private final RestClient restClient;
    private final OpenEmrAuthService openEmrAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    private String getBaseUrl() {
        OpenEmrConfig openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
        return openEmrConfig.getApiUrl();
    }

    // 1. Get All Patients (with optional query filters)
    public Bundle getPatients(Map<String, String> queryParams) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + "/fhir/Patient");
            queryParams.forEach(builder::queryParam);

            String responseBody = restClient
                    .get()
                    .uri(builder.build(true).toUriString())
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            return fhirContext.newJsonParser().parseResource(Bundle.class, responseBody);
        } catch (Exception e) {
            log.error("Failed to fetch patients: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch patients", e);
        }
    }

    public Bundle getAllPatients() {
        return getPatients(Map.of());
    }

    // 2. Simplified Patient List for frontend UI
    public List<Map<String, String>> getSimplifiedPatients() {
        Bundle bundle = getAllPatients();
        return bundle.getEntry().stream()
                .map(entry -> {
                    Patient p = (Patient) entry.getResource();
                    Map<String, String> m = new HashMap<>();
                    m.put("id", p.getIdElement().getIdPart());
                    m.put("name", p.getName().isEmpty() ? "Unnamed" : p.getName().get(0).getNameAsSingleString());
                    m.put("birthDate", p.getBirthDate() != null ? p.getBirthDate().toString() : "");
                    m.put("gender", p.getGender() != null ? p.getGender().toCode() : "unknown");
                    return m;
                }).toList();
    }

    // 3. Get Patient by UUID
    public Patient getPatientByUuid(String uuid) {
        try {
            String url = getBaseUrl() + "/fhir/Patient/" + uuid;

            String responseBody = restClient
                    .get()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            return fhirContext.newJsonParser().parseResource(Patient.class, responseBody);
        } catch (Exception e) {
            log.error("Failed to fetch patient UUID {}: {}", uuid, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch patient", e);
        }
    }

    // 4. Create Patient
    public Patient createPatient(Patient patientResource) {
        String url = null;
        try {
            OpenEmrConfig openEmrConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            url = openEmrConfig.getApiUrl() + "/fhir/Patient";

            // 🔐 Log token being used
            String token = openEmrAuthService.getCachedAccessToken();
            String responseBody = null;
            log.debug("FHIR response body: {}", responseBody);


            IParser parser = fhirContext.newJsonParser();
            String patientJson = parser.encodeResourceToString(patientResource);

            responseBody = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patientJson)
                    .retrieve()
                    .body(String.class);

            return parser.parseResource(Patient.class, responseBody);
        } catch (Exception e) {
            log.error("❌ Failed to create patient: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    // 5. Update Patient
    public Patient updatePatient(String uuid, Patient patientResource) {
        try {
            String url = getBaseUrl() + "/fhir/Patient/" + uuid;
            String patientJson = fhirContext.newJsonParser().encodeResourceToString(patientResource);

            String responseBody = restClient
                    .method(HttpMethod.PUT)
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(patientJson)
                    .retrieve()
                    .body(String.class);

            return fhirContext.newJsonParser().parseResource(Patient.class, responseBody);
        } catch (Exception e) {
            log.error("Failed to update patient: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update patient", e);
        }
    }

    // 6. Save Vitals
    public ApiResponse<Observation> saveVitalSigns(Observation vitalsResource) {
        try {
            String url = getBaseUrl() + "/fhir/VitalSigns";
            String vitalsJson = fhirContext.newJsonParser().encodeResourceToString(vitalsResource);

            String responseBody = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(vitalsJson)
                    .retrieve()
                    .body(String.class);

            Observation saved = fhirContext.newJsonParser().parseResource(Observation.class, responseBody);
            return ApiResponse.<Observation>builder().success(true).data(saved).build();
        } catch (Exception e) {
            log.error("Failed to save vitals: {}", e.getMessage(), e);
            return ApiResponse.<Observation>builder().success(false).message(e.getMessage()).build();
        }
    }

    // 7. Create Bill
    public ApiResponse<Invoice> createPatientBill(Invoice billResource) {
        try {
            String url = getBaseUrl() + "/fhir/PatientBill";
            String billJson = fhirContext.newJsonParser().encodeResourceToString(billResource);

            String responseBody = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(billJson)
                    .retrieve()
                    .body(String.class);

            Invoice created = fhirContext.newJsonParser().parseResource(Invoice.class, responseBody);
            return ApiResponse.<Invoice>builder().success(true).data(created).build();
        } catch (Exception e) {
            log.error("Failed to create bill: {}", e.getMessage(), e);
            return ApiResponse.<Invoice>builder().success(false).message(e.getMessage()).build();
        }
    }

    // 8. Record Payment
    public ApiResponse<PaymentReconciliation> createPayment(PaymentReconciliation paymentResource) {
        try {
            String url = getBaseUrl() + "/fhir/Payment";
            String paymentJson = fhirContext.newJsonParser().encodeResourceToString(paymentResource);

            String responseBody = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + openEmrAuthService.getCachedAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(paymentJson)
                    .retrieve()
                    .body(String.class);

            PaymentReconciliation saved = fhirContext.newJsonParser().parseResource(PaymentReconciliation.class, responseBody);
            return ApiResponse.<PaymentReconciliation>builder().success(true).data(saved).build();
        } catch (Exception e) {
            log.error("Failed to create payment: {}", e.getMessage(), e);
            return ApiResponse.<PaymentReconciliation>builder().success(false).message(e.getMessage()).build();
        }
    }
}
