package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.CoverageResponseDTO;
import com.qiaben.ciyex.service.OpenEmrFhirCoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/openemr")
@RequiredArgsConstructor
public class OpenEmrFhirCoverageController {

    private final OpenEmrFhirCoverageService fhirService;

    @GetMapping("/coverage")
    public ResponseEntity<CoverageResponseDTO> getCoverage(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String payor) {
        CoverageResponseDTO response = fhirService.getCoverage(_id, _lastUpdated, patient, payor);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/coverage/{uuid}")
    public ResponseEntity<Map<String, Object>> getCoverageByUuid(@PathVariable String uuid) {
        Map<String, Object> coverage = fhirService.getCoverageByUuid(uuid);
        return ResponseEntity.ok(coverage);
    }
}
