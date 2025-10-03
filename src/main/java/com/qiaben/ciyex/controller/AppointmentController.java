package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.AppointmentDTO;
import com.qiaben.ciyex.security.RequireScope;
import com.qiaben.ciyex.service.AppointmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// support both non-versioned and v1 paths so existing clients keep working
@RequestMapping({"/api/appointments", "/api/v1/appointments"})
@RequiredArgsConstructor
@Slf4j
@RequireScope("appointments:read")  // Default scope for appointment operations
public class AppointmentController {

    private final AppointmentService service;

    // -------- Create --------
    @PostMapping
    @RequireScope("appointments:write")
    public ResponseEntity<ApiResponse<AppointmentDTO>> create(@RequestBody AppointmentDTO dto) {
        try {
            AppointmentDTO created = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AppointmentDTO>builder()
                    .success(true)
                    .message("Appointment created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create appointment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<AppointmentDTO>builder()
                    .success(false)
                    .message("Failed to create appointment: " + e.getMessage())
                    .build());
        }
    }

    // -------- Retrieve --------
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentDTO>> get(@PathVariable Long id) {
        try {
            AppointmentDTO appointment = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                    .success(true)
                    .message("Appointment retrieved successfully")
                    .data(appointment)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve appointment with id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<AppointmentDTO>builder()
                    .success(false)
                    .message("Failed to retrieve appointment: " + e.getMessage())
                    .build());
        }
    }

    // -------- Current patient (paginated) --------

    // -------- Current patient (non-paginated list) --------


    // -------- Update --------
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentDTO>> update(@PathVariable Long id, @RequestBody AppointmentDTO dto) {
        try {
            AppointmentDTO updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                    .success(true)
                    .message("Appointment updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update appointment with id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<AppointmentDTO>builder()
                    .success(false)
                    .message("Failed to update appointment: " + e.getMessage())
                    .build());
        }
    }

    // -------- Delete --------
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Appointment deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete appointment with id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete appointment: " + e.getMessage())
                    .build());
        }
    }

    // -------- List all (paginated) --------
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AppointmentDTO>>> getAll(@PageableDefault Pageable pageable) {
        try {
            Page<AppointmentDTO> appointments = service.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.<Page<AppointmentDTO>>builder()
                    .success(true)
                    .message("Appointments retrieved successfully")
                    .data(appointments)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve appointments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<Page<AppointmentDTO>>builder()
                    .success(false)
                    .message("Failed to retrieve appointments: " + e.getMessage())
                    .build());
        }
    }

    // -------- List by patient (paginated) --------
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Page<AppointmentDTO>>> getByPatient(
            @PathVariable Long patientId,
            @PageableDefault Pageable pageable) {
        try {
            Page<AppointmentDTO> appointments = service.getByPatientId(patientId, pageable);
            return ResponseEntity.ok(ApiResponse.<Page<AppointmentDTO>>builder()
                    .success(true)
                    .message("Appointments retrieved successfully for patient " + patientId)
                    .data(appointments)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve appointments for patient {}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<Page<AppointmentDTO>>builder()
                    .success(false)
                    .message("Failed to retrieve appointments: " + e.getMessage())
                    .build());
        }
    }
    // Count all appointments
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> count() {
        try {
            long count = service.count();
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(true)
                    .message("Appointment count retrieved successfully")
                    .data(count)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve appointment count", e);
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .success(false)
                    .message("Failed to retrieve appointment count: " + e.getMessage())
                    .build());
        }
    }

}
