package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.AppointmentDTO;
import org.ciyex.ehr.service.AppointmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
// support both non-versioned and v1 paths so existing clients keep working
@RequestMapping({"/api/appointments", "/api/v1/appointments"})
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService service;

    // -------- Create --------
    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentDTO>> create(@RequestBody AppointmentDTO dto) {
        try {
            AppointmentDTO created = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AppointmentDTO>builder()
                    .success(true)
                    .message("Appointment created successfully")
                    .data(created)
                    .build());
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating appointment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<AppointmentDTO>builder()
                    .success(false)
                    .message(e.getMessage())
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
    public ResponseEntity<ApiResponse<AppointmentDTO>> get(@PathVariable("id") Long id) {
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
    public ResponseEntity<ApiResponse<AppointmentDTO>> update(@PathVariable("id") Long id, @RequestBody AppointmentDTO dto) {
        try {
            AppointmentDTO updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                    .success(true)
                    .message("Appointment updated successfully")
                    .data(updated)
                    .build());
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating appointment with id {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.<AppointmentDTO>builder()
                    .success(false)
                    .message(e.getMessage())
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
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") Long id) {
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

    // -------- Delete by patient --------
    @DeleteMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Void>> deleteByPatient(@PathVariable("patientId") Long patientId) {
        try {
            service.deleteByPatientId(patientId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("All appointments deleted successfully for patient " + patientId)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete appointments for patient {}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete appointments: " + e.getMessage())
                    .build());
        }
    }

    // -------- List all (paginated) --------
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AppointmentDTO>>> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        try {
            Page<AppointmentDTO> appointments = service.getAll(org.springframework.data.domain.PageRequest.of(page, size));
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
            @PathVariable("patientId") Long patientId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        try {
            Page<AppointmentDTO> appointments = service.getByPatientId(patientId, org.springframework.data.domain.PageRequest.of(page, size));
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

    // -------- Get latest appointment for patient --------
    @GetMapping("/patient/{patientId}/latest")
    public ResponseEntity<ApiResponse<AppointmentDTO>> getLatestByPatient(@PathVariable("patientId") Long patientId) {
        try {
            AppointmentDTO latest = service.getLatestByPatientId(patientId);
            if (latest == null) {
                return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                        .success(true)
                        .message("No appointments found for patient " + patientId)
                        .data(null)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                    .success(true)
                    .message("Latest appointment retrieved successfully")
                    .data(latest)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve latest appointment for patient {}", patientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.<AppointmentDTO>builder()
                    .success(false)
                    .message("Failed to retrieve latest appointment: " + e.getMessage())
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
    // -------------------- Update STATUS only (for the UI dropdown) --------------------
    // PUT /api/appointments/{id}/status   body: { "status": "Checked" | "Unchecked" }
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentDTO>> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body
    ) {
        try {
            String status = body.get("status");
            AppointmentDTO updated = service.updateStatus(id, status);
            return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                    .success(true)
                    .message("Status updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update status for appointment {}", id, e);
            return ResponseEntity.ok(ApiResponse.<AppointmentDTO>builder()
                    .success(false)
                    .message("Failed to update status: " + e.getMessage())
                    .build());
        }
    }

}
