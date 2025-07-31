package com.qiaben.ciyex.controller.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.core.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Bundle;
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
        log.info("Received request to create appointment");
        ApiResponse<Appointment> response = appointmentService.create(appointment);

        if (response.isSuccess() && response.getData() != null) {
            String json = fhirContext.newJsonParser()
                    .setPrettyPrint(true)
                    .encodeResourceToString(response.getData());
            return ResponseEntity.ok(json);
        } else {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"" + response.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

    @GetMapping("/list")
    public ResponseEntity<String> getAppointments(
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String lastUpdated
    ) {
        try {
            Bundle result = appointmentService.getAppointments(patient, lastUpdated);
            String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(result);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<String> getAppointmentByUuid(@PathVariable String uuid) {
        try {
            Appointment appointment = appointmentService.getAppointmentByUuid(uuid);
            String json = fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(appointment);
            return ResponseEntity.ok(json);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/count")
    public long getAppointmentCount() {
        return appointmentService.countAppointments();
    }
}
