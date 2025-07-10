package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.FhirMetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir")

public class FhirMetadataController {

    private final FhirMetadataService fhirMetadataService;

    public FhirMetadataController(FhirMetadataService fhirMetadataService) {
        this.fhirMetadataService = fhirMetadataService;
    }

    @GetMapping("/metadata")
    public ResponseEntity<Object> getMetadata() {
        return fhirMetadataService.getMetadata();
    }

    @GetMapping("/.well-known/smart-configuration")
    public ResponseEntity<Object> getSmartConfiguration() {
        return fhirMetadataService.getSmartConfiguration();
    }
}