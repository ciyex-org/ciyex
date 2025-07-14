package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.service.fhir.FhirExportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir/$export")

public class FhirExportController {

    private final FhirExportService fhirExportService;

    public FhirExportController(FhirExportService fhirExportService) {
        this.fhirExportService = fhirExportService;
    }

    @GetMapping
    public ResponseEntity<Object> getBulkExportData() {
        return fhirExportService.getBulkExportData();
    }
}