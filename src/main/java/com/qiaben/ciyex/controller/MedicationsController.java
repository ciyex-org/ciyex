package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.MedicationsService;
import com.qiaben.ciyex.dto.MedicationRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/medications", "/api/fhir/medications"})
public class MedicationsController {

    private final MedicationsService medicationsService;

    public MedicationsController(MedicationsService medicationsService) {
        this.medicationsService = medicationsService;
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ResponseEntity<?> getMyMedications() {
        try {
            List<MedicationRequestDto> medications = medicationsService.getMedicationsForPortalUser();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Patient medications retrieved",
                "data", medications
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Unable to load medications data",
                "error", e.getMessage()
            ));
        }
    }
}