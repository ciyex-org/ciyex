package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.AllergyIntoleranceResponseDTO;
import com.qiaben.ciyex.service.fhir.FhirAllergyIntoleranceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fhir")
@RequiredArgsConstructor
public class FhirAllergyIntoleranceController {

    private final FhirAllergyIntoleranceService allergyIntoleranceService;

    // Endpoint to fetch a list of AllergyIntolerance resources
    @GetMapping("/AllergyIntolerance")
    public ResponseEntity<List<AllergyIntoleranceResponseDTO>> getAllergyIntolerances(
            @RequestParam Map<String, String> queryParams) {

        // Get the list of AllergyIntolerances from the service
        List<AllergyIntoleranceResponseDTO> allergyIntolerances = allergyIntoleranceService.getAllergyIntolerances(queryParams);

        return ResponseEntity.ok(allergyIntolerances);
    }

    // Endpoint to fetch a single AllergyIntolerance resource by UUID
    @GetMapping("/AllergyIntolerance/{uuid}")
    public ResponseEntity<AllergyIntoleranceResponseDTO> getAllergyIntolerance(@PathVariable String uuid) {
        AllergyIntoleranceResponseDTO allergyIntoleranceResponse = allergyIntoleranceService.getAllergyIntolerance(uuid);
        if (allergyIntoleranceResponse == null) {
            return ResponseEntity.status(404).body(null);  // 404 if not found
        }

        return ResponseEntity.ok(allergyIntoleranceResponse);
    }
}