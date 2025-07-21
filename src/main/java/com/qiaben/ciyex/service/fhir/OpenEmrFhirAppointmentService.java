package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.AppointmentResponseDTO;
import com.qiaben.ciyex.dto.fhir.AppointmentResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class OpenEmrFhirAppointmentService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient            restClient;
    private final OpenEmrAuthService    openEmrAuthService;

    /* -------------------------------------------------- GET LIST */
    public AppointmentResponseDTO getAppointments(String patientId, String lastUpdated) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Appointment";

            if (patientId != null && !patientId.isBlank()) {
                url += "?patient=" + patientId;
            }
            if (lastUpdated != null && !lastUpdated.isBlank()) {
                url += (url.contains("?") ? "&" : "?") + "_lastUpdated=" + lastUpdated;
            }

            ResponseEntity<AppointmentResponseDTO> response = restClient.get()
                    .uri(url)
                    .header("Authorization", bearer())
                    .retrieve()
                    .toEntity(AppointmentResponseDTO.class);

            return response.getBody();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch appointments", ex);
        }
    }

    /* -------------------------------------------------- GET BY UUID */
    public AppointmentResponseDTO getAppointmentByUuid(String uuid) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Appointment/" + uuid;

            ResponseEntity<AppointmentResponseDTO> response = restClient.get()
                    .uri(url)
                    .header("Authorization", bearer())
                    .retrieve()
                    .toEntity(AppointmentResponseDTO.class);

            return response.getBody();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch appointment " + uuid, ex);
        }
    }

    /* -------------------------------------------------- CREATE (POST) */
    public AppointmentResponseDTO createAppointment(AppointmentResponseDTO body) {
        try {
            String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Appointment";

            ResponseEntity<AppointmentResponseDTO> response = restClient.post()
                    .uri(url)
                    .header("Authorization", bearer())
                    .body(body)
                    .retrieve()
                    .toEntity(AppointmentResponseDTO.class);

            return response.getBody();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create appointment", ex);
        }
    }

    /* -------------------------------------------------- HELPER */
    /** Builds “Bearer &lt;token&gt;” and handles checked‑exception propagation neatly. */
    private String bearer() {
        try {
            return "Bearer " + openEmrAuthService.getCachedAccessToken();
        } catch (Exception ex) {
            // wrap checked Exception into unchecked to satisfy RestClient header()
            throw new RuntimeException("Unable to obtain OpenEMR access token", ex);
        }
    }
}
