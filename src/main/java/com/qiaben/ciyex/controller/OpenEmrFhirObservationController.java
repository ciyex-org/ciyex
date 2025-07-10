package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ObservationResponseDTO;
import com.qiaben.ciyex.service.OpenEmrFhirObservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/openemr")
@RequiredArgsConstructor
public class OpenEmrFhirObservationController {

    private final OpenEmrFhirObservationService observationService;

    @GetMapping("/observation")
    public ResponseEntity<ObservationResponseDTO> getObservations(@RequestParam Map<String, String> queryParams) {
        ObservationResponseDTO response = observationService.getObservations(queryParams);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/observation/{uuid}")
    public ResponseEntity<Map<String, Object>> getObservationByUuid(@PathVariable String uuid) {
        Map<String, Object> response = observationService.getObservationByUuid(uuid);
        return ResponseEntity.ok(response);
    }
}
