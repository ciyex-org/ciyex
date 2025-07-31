package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.FhirConfig;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirAppointmentService {

    private final RestClient restClient;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();
    // Inject your FHIR AuthService here if you have one:
    // private final FhirAuthService fhirAuthService;

    /** Get a list of appointments (as a Bundle) by patient and/or lastUpdated. */
    public Bundle getAppointments(String patientId, String lastUpdated) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            StringBuilder url = new StringBuilder(fhirConfig.getApiUrl() + "/Appointment");
            boolean first = true;
            if (patientId != null && !patientId.isBlank()) {
                url.append(first ? "?" : "&").append("patient=").append(patientId);
                first = false;
            }
            if (lastUpdated != null && !lastUpdated.isBlank()) {
                url.append(first ? "?" : "&").append("_lastUpdated=").append(lastUpdated);
            }

            log.info("[Org:{}] Requesting appointments with URL: {}", orgId, url);

            String response = restClient.get()
                    .uri(url.toString())
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(String.class);

            log.debug("[Org:{}] FHIR Appointment list response: {}", orgId, response);

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, response);

            int total = bundle.getEntry() != null ? bundle.getEntry().size() : 0;
            log.info("[Org:{}] Appointments fetched: {}", orgId, total);

            return bundle;
        } catch (Exception ex) {
            log.error("[Org:{}] Failed to fetch appointments: {}", orgId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to fetch appointments", ex);
        }
    }

    /** Get a single appointment by UUID. */
    public Appointment getAppointmentByUuid(String uuid) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String url = fhirConfig.getApiUrl() + "/Appointment/" + uuid;

            log.info("[Org:{}] Requesting appointment by UUID: {} (URL: {})", orgId, uuid, url);

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(String.class);

            log.debug("[Org:{}] FHIR Appointment UUID {} response: {}", orgId, uuid, response);

            IParser parser = fhirContext.newJsonParser();
            Appointment appt = parser.parseResource(Appointment.class, response);

            log.info("[Org:{}] Appointment UUID {} fetched successfully", orgId, uuid);
            return appt;
        } catch (Exception ex) {
            log.error("[Org:{}] Failed to fetch appointment {}: {}", orgId, uuid, ex.getMessage(), ex);
            throw new RuntimeException("Failed to fetch appointment " + uuid, ex);
        }
    }

    /** Create a new appointment (POST). */
    public Appointment createAppointment(Appointment appointment) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            FhirConfig fhirConfig = integrationConfigProvider.getForCurrentOrg(IntegrationKey.FHIR);
            String url = fhirConfig.getApiUrl() + "/Appointment";

            IParser parser = fhirContext.newJsonParser();
            String requestBody = parser.encodeResourceToString(appointment);

            log.info("[Org:{}] Creating appointment via URL: {}", orgId, url);
            log.debug("[Org:{}] Appointment POST body: {}", orgId, requestBody);

            String response = restClient.post()
                    .uri(url)
                    .header("Authorization", bearer())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            log.debug("[Org:{}] FHIR Appointment creation response: {}", orgId, response);

            Appointment created = parser.parseResource(Appointment.class, response);

            log.info("[Org:{}] Appointment created with id: {}", orgId, created.getIdElement().getIdPart());
            return created;
        } catch (Exception ex) {
            log.error("[Org:{}] Failed to create appointment: {}", orgId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to create appointment", ex);
        }
    }

    /** Helper for Bearer token */
    private String bearer() {
        // Replace with your own logic for FHIR access token retrieval!
        // return "Bearer " + fhirAuthService.getCachedAccessToken();
        throw new UnsupportedOperationException("Implement FHIR access token logic");
    }
}
