package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirMedicationDto;
import com.qiaben.ciyex.service.FhirMedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fhir/Medication")
@RequiredArgsConstructor
public class FhirMedicationController {

    private final FhirMedicationService medicationService;

    @GetMapping
    public ResponseEntity<FhirMedicationDto> getMedications(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated) {
        return ResponseEntity.ok(medicationService.getMedications(_id, _lastUpdated));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<FhirMedicationDto> getMedicationByUuid(@PathVariable String uuid) {
        return ResponseEntity.ok(medicationService.getMedicationByUuid(uuid));
    }
}
