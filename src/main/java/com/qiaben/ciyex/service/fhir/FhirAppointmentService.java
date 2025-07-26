package com.qiaben.ciyex.service.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.qiaben.ciyex.dto.core.integration.IntegrationKey;
import com.qiaben.ciyex.dto.core.integration.OpenEmrConfig;
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
    private final OpenEmrAuthService openEmrAuthService;
    private final OrgIntegrationConfigProvider integrationConfigProvider;
    private final FhirContext fhirContext = FhirContext.forR4();

    public Appointment createAppointment(Appointment appointment) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            OpenEmrConfig config = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String url = config.getApiUrl() + "/fhir/Appointment";

            String body = fhirContext.newJsonParser().encodeResourceToString(appointment);

            String response = restClient.post()
                    .uri(url)
                    .header("Authorization", bearer())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return fhirContext.newJsonParser().parseResource(Appointment.class, response);
        } catch (Exception ex) {
            log.error("[Org:{}] Failed to create appointment: {}", orgId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to create appointment", ex);
        }
    }

    public Bundle getAppointments(String patientId, String lastUpdated) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            OpenEmrConfig config = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            StringBuilder url = new StringBuilder(config.getApiUrl() + "/fhir/Appointment");
            boolean first = true;

            if (patientId != null && !patientId.isBlank()) {
                url.append(first ? "?" : "&").append("patient=").append(patientId);
                first = false;
            }
            if (lastUpdated != null && !lastUpdated.isBlank()) {
                url.append(first ? "?" : "&").append("_lastUpdated=").append(lastUpdated);
            }

            String response = restClient.get()
                    .uri(url.toString())
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(String.class);

            return fhirContext.newJsonParser().parseResource(Bundle.class, response);
        } catch (Exception ex) {
            log.error("[Org:{}] Failed to fetch appointments: {}", orgId, ex.getMessage(), ex);
            throw new RuntimeException("Failed to fetch appointments", ex);
        }
    }

    public Appointment getAppointmentByUuid(String uuid) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        try {
            OpenEmrConfig config = integrationConfigProvider.getForCurrentOrg(IntegrationKey.OPENEMR);
            String url = config.getApiUrl() + "/fhir/Appointment/" + uuid;

            String response = restClient.get()
                    .uri(url)
                    .header("Authorization", bearer())
                    .retrieve()
                    .body(String.class);

            return fhirContext.newJsonParser().parseResource(Appointment.class, response);
        } catch (Exception ex) {
            log.error("[Org:{}] Failed to fetch appointment {}: {}", orgId, uuid, ex.getMessage(), ex);
            throw new RuntimeException("Failed to fetch appointment", ex);
        }
    }

    private String bearer() {
        try {
            return "Bearer " + openEmrAuthService.getCachedAccessToken();
        } catch (Exception ex) {
            Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
            log.error("[Org:{}] Unable to obtain OpenEMR access token: {}", orgId, ex.getMessage(), ex);
            throw new RuntimeException("Unable to obtain OpenEMR access token", ex);
        }
    }
}
