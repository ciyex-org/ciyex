package com.qiaben.ciyex.controller.core;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.core.AppointmentDTO;
import com.qiaben.ciyex.dto.fhir.AppointmentResponseDTO;
import com.qiaben.ciyex.service.core.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<AppointmentResponseDTO>> createAppointment(
            @Valid @RequestBody AppointmentDTO appointment) {
        ApiResponse<AppointmentResponseDTO> response = appointmentService.create(appointment);
        return ResponseEntity.ok(response);
    }
}

