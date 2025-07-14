package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.service.fhir.FhirBulkDataStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fhir/$bulkdata-status")

public class FhirBulkDataStatusController {

    private final FhirBulkDataStatusService fhirBulkDataStatusService;

    public FhirBulkDataStatusController(FhirBulkDataStatusService fhirBulkDataStatusService) {
        this.fhirBulkDataStatusService = fhirBulkDataStatusService;
    }

    @GetMapping
    public ResponseEntity<Object> getBulkDataStatus() {
        return fhirBulkDataStatusService.getBulkDataStatus();
    }

    @DeleteMapping
    public ResponseEntity<Object> deleteBulkDataStatus() {
        return fhirBulkDataStatusService.deleteBulkDataStatus();
    }
}