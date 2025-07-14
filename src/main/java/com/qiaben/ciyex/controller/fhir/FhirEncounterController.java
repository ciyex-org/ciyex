package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.FhirEncounterDTO;
import com.qiaben.ciyex.service.fhir.FhirEncounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;  // <-- Add this import
import java.util.Map;

@RestController
@RequestMapping("/portal/patient/encounter")
@RequiredArgsConstructor
public class FhirEncounterController {

    private final FhirEncounterService encounterService;

    // Endpoint to fetch all encounters for a patient
    @GetMapping
    public ResponseEntity<List<FhirEncounterDTO>> getEncounters(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated,
            @RequestParam(required = false) String patient
    ) {
        Map<String, String> queryParams = Map.of(
                "_id", _id,
                "_lastUpdated", _lastUpdated,
                "patient", patient
        );

        List<FhirEncounterDTO> encounters = encounterService.getEncounters(queryParams);
        return ResponseEntity.ok(encounters);
    }

    // Endpoint to fetch a specific encounter by UUID
    @GetMapping("/{euuid}")
    public ResponseEntity<FhirEncounterDTO> getEncounterByUuid(@PathVariable String euuid) {
        FhirEncounterDTO encounter = encounterService.getEncounterByUuid(euuid);
        if (encounter == null) {
            return ResponseEntity.status(404).build();  // Not Found
        }
        return ResponseEntity.ok(encounter);
    }
}
