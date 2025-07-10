package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.FhirCarePlanResponseDTO;
import com.qiaben.ciyex.service.FhirCarePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fhir")
@RequiredArgsConstructor
public class FhirCarePlanController {

    private final FhirCarePlanService carePlanService;

    // Endpoint to fetch a list of CarePlan resources
    @GetMapping("/CarePlan")
    public ResponseEntity<List<FhirCarePlanResponseDTO>> getCarePlans(@RequestParam Map<String, String> queryParams) {
        // Get the list of CarePlans from the service
        List<FhirCarePlanResponseDTO> carePlans = carePlanService.getCarePlans(queryParams);

        return ResponseEntity.ok(carePlans);
    }

    // Endpoint to fetch a single CarePlan resource by UUID
    @GetMapping("/CarePlan/{uuid}")
    public ResponseEntity<FhirCarePlanResponseDTO> getCarePlan(@PathVariable String uuid) {
        FhirCarePlanResponseDTO carePlanResponse = carePlanService.getCarePlan(uuid);
        if (carePlanResponse == null) {
            return ResponseEntity.status(404).body(null);  // 404 if not found
        }

        return ResponseEntity.ok(carePlanResponse);
    }
}
