package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirDiagnosticReportDTO;
import com.qiaben.ciyex.service.FhirDiagnosticReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/fhir/DiagnosticReport")
@RequiredArgsConstructor
public class FhirDiagnosticReportController {

    private final FhirDiagnosticReportService diagnosticReportService;

    // Endpoint to fetch all DiagnosticReport resources
    @GetMapping
    public ResponseEntity<FhirDiagnosticReportDTO> getDiagnosticReports(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String date
    ) {
        Map<String, String> queryParams = Map.of(
                "_id", _id,
                "_lastUpdated", _lastUpdated,
                "patient", patient,
                "code", code,
                "category", category,
                "date", date
        );

        FhirDiagnosticReportDTO diagnosticReportDTO = diagnosticReportService.getDiagnosticReports(queryParams);
        return ResponseEntity.ok(diagnosticReportDTO);
    }

    // Endpoint to fetch a single DiagnosticReport by UUID
    @GetMapping("/{uuid}")
    public ResponseEntity<FhirDiagnosticReportDTO> getDiagnosticReportByUuid(@PathVariable String uuid) {
        FhirDiagnosticReportDTO diagnosticReportDTO = diagnosticReportService.getDiagnosticReportByUuid(uuid);
        if (diagnosticReportDTO == null) {
            return ResponseEntity.status(404).build();  // Not Found
        }
        return ResponseEntity.ok(diagnosticReportDTO);
    }
}
