package org.ciyex.ehr.kiosk.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.kiosk.dto.KioskCheckinDto;
import org.ciyex.ehr.kiosk.dto.KioskConfigDto;
import org.ciyex.ehr.kiosk.service.KioskService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@PreAuthorize("hasAuthority('SCOPE_user/Appointment.read')")
@RestController
@RequestMapping("/api/kiosk")
@RequiredArgsConstructor
@Slf4j
public class KioskController {

    private final KioskService service;

    // ─── Config ───

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<KioskConfigDto>> getConfig() {
        try {
            var config = service.getConfig();
            return ResponseEntity.ok(ApiResponse.ok("Kiosk config retrieved", config));
        } catch (Exception e) {
            log.error("Failed to get kiosk config", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/config")
    @PreAuthorize("hasAuthority('SCOPE_user/Appointment.write')")
    public ResponseEntity<ApiResponse<KioskConfigDto>> saveConfig(@RequestBody KioskConfigDto dto) {
        try {
            var saved = service.saveConfig(dto);
            return ResponseEntity.ok(ApiResponse.ok("Kiosk config saved", saved));
        } catch (Exception e) {
            log.error("Failed to save kiosk config", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ─── Check-in ───

    @PostMapping("/checkin")
    @PreAuthorize("hasAuthority('SCOPE_user/Appointment.write')")
    public ResponseEntity<ApiResponse<KioskCheckinDto>> checkIn(@RequestBody KioskCheckinDto dto) {
        try {
            var checkin = service.checkIn(dto);
            return ResponseEntity.ok(ApiResponse.ok("Check-in recorded", checkin));
        } catch (Exception e) {
            log.error("Failed to record check-in", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/checkins")
    public ResponseEntity<ApiResponse<Page<KioskCheckinDto>>> getTodayCheckins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkInTime"));
            var checkins = service.getTodayCheckins(pageable);
            return ResponseEntity.ok(ApiResponse.ok("Today's check-ins retrieved", checkins));
        } catch (Exception e) {
            log.error("Failed to get today's check-ins", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/checkins/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<KioskCheckinDto>>> getByPatient(@PathVariable Long patientId) {
        try {
            var checkins = service.getCheckinsByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient check-ins retrieved", checkins));
        } catch (Exception e) {
            log.error("Failed to get check-ins for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        try {
            var stats = service.todayStats();
            return ResponseEntity.ok(ApiResponse.ok("Kiosk stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get kiosk stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
