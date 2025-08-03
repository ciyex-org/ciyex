package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.storage.fhir.FhirAppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final FhirAppointmentService fhirAppointmentService;

    public ApiResponse<Appointment> create(Appointment appointment) {
        try {
            String patientReference = getPatientReference(appointment);
            log.info("Creating appointment for patient: {}", patientReference);

            Appointment response = fhirAppointmentService.createAppointment(appointment);

            return ApiResponse.<Appointment>builder()
                    .success(true)
                    .data(response)
                    .message("Appointment created successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to create appointment: {}", e.getMessage(), e);
            return ApiResponse.<Appointment>builder()
                    .success(false)
                    .message("Failed to create appointment: " + e.getMessage())
                    .build();
        }
    }

    public Bundle getAppointments(String patientId, String lastUpdated) {
        return fhirAppointmentService.getAppointments(patientId, lastUpdated);
    }

    public Appointment getAppointmentByUuid(String uuid) {
        return fhirAppointmentService.getAppointmentByUuid(uuid);
    }

    private String getPatientReference(Appointment appointment) {
        if (appointment == null || appointment.getParticipant() == null) {
            return "Unknown";
        }
        return appointment.getParticipant().stream()
                .filter(p -> p.hasActor() && p.getActor().getReference() != null &&
                        p.getActor().getReference().startsWith("Patient/"))
                .map(p -> p.getActor().getReference())
                .findFirst()
                .orElse("Unknown");
    }
    public long countAppointments() {
        try {
            Bundle bundle = fhirAppointmentService.getAppointments(null, null); // null = get all
            long count = bundle.getEntry().stream()
                    .filter(entry -> entry.getResource() instanceof Appointment)
                    .count();
            log.info("Fetched {} appointments", count);
            return count;
        } catch (Exception e) {
            log.error("Error counting appointments: {}", e.getMessage(), e);
            return 0;
        }
    }
}
