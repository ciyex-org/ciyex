package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirValueSetSearchParamsDto;
import com.qiaben.ciyex.service.FhirValueSetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fhir/ValueSet")

public class FhirValueSetController {

    private final FhirValueSetService fhirValueSetService;

    public FhirValueSetController(FhirValueSetService fhirValueSetService) {
        this.fhirValueSetService = fhirValueSetService;
    }

    @GetMapping
    public ResponseEntity<Object> getValueSets(FhirValueSetSearchParamsDto params) {
        return fhirValueSetService.getValueSets(params);
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Object> getValueSetById(@PathVariable String uuid) {
        return fhirValueSetService.getValueSetById(uuid);
    }
}