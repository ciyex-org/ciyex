package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.AppointmentResponseDTO;
import com.qiaben.ciyex.service.fhir.OpenEmrFhirAppointmentService;
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
