package com.qiaben.ciyex.service.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.fhir.FhirAppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Appointment;
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

            log.info("Appointment created with ID: {}", response.getIdElement().getIdPart());

            return ApiResponse.<Appointment>builder()
                    .success(true)
                    .data(response)
                    .message("Appointment created successfully")
                    .build();
        } catch (Exception e) {
            log.error("Failed to create appointment. Error: {}", e.getMessage(), e);
            return ApiResponse.<Appointment>builder()
                    .success(false)
                    .message("Failed to create appointment: " + e.getMessage())
                    .build();
        }
    }

    private String getPatientReference(Appointment appointment) {
        if (appointment == null || appointment.getParticipant() == null) {
            return "Unknown";
        }
        for (Appointment.AppointmentParticipantComponent participant : appointment.getParticipant()) {
            if (participant.hasActor() && participant.getActor().getReference() != null
                    && participant.getActor().getReference().startsWith("Patient/")) {
                return participant.getActor().getReference();
            }
        }
        return "Unknown";
    }
}
