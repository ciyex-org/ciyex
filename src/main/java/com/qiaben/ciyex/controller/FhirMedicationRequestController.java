package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirMedicationRequestDTO;
import com.qiaben.ciyex.service.FhirMedicationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/fhir/MedicationRequest")
@RequiredArgsConstructor
public class FhirMedicationRequestController {

    private final FhirMedicationRequestService medicationRequestService;

    // Endpoint to fetch all MedicationRequest resources
    @GetMapping
    public ResponseEntity<FhirMedicationRequestDTO> getMedicationRequests(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String intent,
            @RequestParam(required = false) String status
    ) {
        Map<String, String> queryParams = Map.of(
                "_id", _id,
                "_lastUpdated", _lastUpdated,
                "patient", patient,
                "intent", intent,
                "status", status
        );

        FhirMedicationRequestDTO medicationRequestDTO = medicationRequestService.getMedicationRequests(queryParams);
        return ResponseEntity.ok(medicationRequestDTO);
    }

    // Endpoint to fetch a single MedicationRequest by UUID
    @GetMapping("/{uuid}")
    public ResponseEntity<FhirMedicationRequestDTO> getMedicationRequestByUuid(@PathVariable String uuid) {
        FhirMedicationRequestDTO medicationRequestDTO = medicationRequestService.getMedicationRequestByUuid(uuid);
        if (medicationRequestDTO == null) {
            return ResponseEntity.status(404).build();  // Not Found
        }
        return ResponseEntity.ok(medicationRequestDTO);
    }
}
