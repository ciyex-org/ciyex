package com.qiaben.ciyex.service.fhir;

import com.qiaben.ciyex.config.OpenEmrFhirProperties;
import com.qiaben.ciyex.dto.fhir.AppointmentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.http.ResponseEntity;

@Service
public class OpenEmrFhirAppointmentService {

    private final OpenEmrFhirProperties openEmrFhirProperties;
    private final RestClient restClient;

    @Autowired
    public OpenEmrFhirAppointmentService(OpenEmrFhirProperties openEmrFhirProperties, RestClient restClient) {
        this.openEmrFhirProperties = openEmrFhirProperties;
        this.restClient = restClient;
    }

    public AppointmentResponseDTO getAppointments(String patientId, String lastUpdated) {
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Appointment";

        if (patientId != null) {
            url += "?patient=" + patientId;
        }
        if (lastUpdated != null) {
            url += (url.contains("?") ? "&" : "?") + "_lastUpdated=" + lastUpdated;
        }

        // Using RestClient to make the API call and getting response entity
        ResponseEntity<AppointmentResponseDTO> response = restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + openEmrFhirProperties.getToken())
                .retrieve()
                .toEntity(AppointmentResponseDTO.class); // Get the response entity

        return response.getBody(); // Return the full response including metadata and entry
    }
    // Method for getting a single appointment by uuid
    public AppointmentResponseDTO getAppointmentByUuid(String uuid) {
        String url = openEmrFhirProperties.getBaseUrl() + "/fhir/Appointment/" + uuid;

        // Using RestClient to fetch the single appointment
        ResponseEntity<AppointmentResponseDTO> response = restClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + openEmrFhirProperties.getToken())
                .retrieve()
                .toEntity(AppointmentResponseDTO.class);

        return response.getBody();// Return the single appointment
    }
}
