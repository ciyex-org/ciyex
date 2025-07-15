package com.qiaben.ciyex.controller.core;

import com.qiaben.ciyex.dto.core.AppointmentDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    @PostMapping("/create")
    public ResponseEntity<?> createAppointment(@Valid @RequestBody AppointmentDTO appointment) {
        // Your logic here...
        return ResponseEntity.ok("Appointment created successfully!");
    }
}

