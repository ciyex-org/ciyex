package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.AppointmentResponseDTO;
import com.qiaben.ciyex.service.OpenEmrFhirAppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fhir/Appointment")
public class OpenEmrFhirAppointmentController {

    @Autowired
    private OpenEmrFhirAppointmentService appointmentService;

    @GetMapping
    public AppointmentResponseDTO getAppointments(
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String _lastUpdated) {
        return appointmentService.getAppointments(patient, _lastUpdated);
    }
    @GetMapping("/{uuid}")
    public AppointmentResponseDTO getAppointmentByUuid(@PathVariable String uuid) {
        return appointmentService.getAppointmentByUuid(uuid);
    }
}
