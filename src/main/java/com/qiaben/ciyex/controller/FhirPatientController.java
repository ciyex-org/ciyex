package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirPatientDto;
import com.qiaben.ciyex.dto.FhirPatientListResponseDto;
import com.qiaben.ciyex.dto.FhirPatientSingleResponseDto;
import com.qiaben.ciyex.service.FhirPatientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/openemr/patient")

public class FhirPatientController {
    private final FhirPatientService fhirPatientService;

    public FhirPatientController(FhirPatientService fhirPatientService) {
        this.fhirPatientService = fhirPatientService;
    }


    @GetMapping
    public ResponseEntity<FhirPatientListResponseDto> getPatients(@RequestParam Map<String, String> allParams) {
        FhirPatientListResponseDto dto = fhirPatientService.getPatients(allParams);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<FhirPatientSingleResponseDto> getPatientByUuid(@PathVariable String uuid) {
        return ResponseEntity.ok(fhirPatientService.getPatientByUuid(uuid));
    }

    @PostMapping
    public ResponseEntity<FhirPatientSingleResponseDto> createPatient(@RequestBody FhirPatientDto body) {
        return ResponseEntity.ok(fhirPatientService.createPatient(body));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<FhirPatientSingleResponseDto> updatePatient(
            @PathVariable String uuid, @RequestBody FhirPatientDto body) {
        return ResponseEntity.ok(fhirPatientService.updatePatient(uuid, body));
    }
}
