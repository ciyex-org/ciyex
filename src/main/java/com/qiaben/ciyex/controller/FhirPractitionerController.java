package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirPractitionerRequestDto;
import com.qiaben.ciyex.dto.FhirPractitionerSearchParamsDto;
import com.qiaben.ciyex.service.FhirPractitionerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fhir/Practitioner")

public class FhirPractitionerController {

    private final FhirPractitionerService fhirPractitionerService;

    public FhirPractitionerController(FhirPractitionerService fhirPractitionerService) {
        this.fhirPractitionerService = fhirPractitionerService;
    }

    @GetMapping
    public ResponseEntity<Object> getPractitioners(FhirPractitionerSearchParamsDto params) {
        return fhirPractitionerService.getPractitioners(params);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Object> getPractitionerById(@PathVariable String uuid) {
        return fhirPractitionerService.getPractitionerById(uuid);
    }

    @PostMapping
    public ResponseEntity<Object> createPractitioner(@RequestBody FhirPractitionerRequestDto dto) {
        return fhirPractitionerService.createPractitioner(dto);
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<Object> updatePractitioner(@PathVariable String uuid, @RequestBody FhirPractitionerRequestDto dto) {
        return fhirPractitionerService.updatePractitioner(uuid, dto);
    }
}