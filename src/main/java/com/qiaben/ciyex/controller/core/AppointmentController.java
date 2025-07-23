package com.qiaben.ciyex.controller.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.core.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Appointment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ca.uhn.fhir.context.FhirContext;

@RestController
@RequestMapping("/api/appointment")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final FhirContext fhirContext = FhirContext.forR4();

    @PostMapping("/create")
    public ResponseEntity<String> createAppointment(@Valid @RequestBody Appointment appointment) {
        String patientRef = "null";
     /*   if (appointment.hasSubject() && appointment.getSubject() != null) {
            patientRef = appointment.getSubject().getReference();
        }*/

        log.info("Received request to create appointment for patient ref: {}", patientRef);

        ApiResponse<Appointment> response = appointmentService.create(appointment);

        if (response.isSuccess() && response.getData() != null) {
            String json = fhirContext.newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(response.getData());
            log.info("Appointment created successfully with ID: {}", response.getData().getIdElement().getIdPart());
            return ResponseEntity.ok(json);
        } else {
            log.error("Failed to create appointment: {}", response.getMessage());
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"" + response.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

}
