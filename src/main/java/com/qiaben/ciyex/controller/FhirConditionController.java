package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.FhirConditionService;
import com.qiaben.ciyex.dto.FhirConditionResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fhir")
public class FhirConditionController {

    private final FhirConditionService conditionService;

    public FhirConditionController(FhirConditionService conditionService) {
        this.conditionService = conditionService;
    }

    @GetMapping("/Condition")
    public ResponseEntity<FhirConditionResponseDTO> getConditions(
            @RequestParam(value = "_id", required = false) String id,
            @RequestParam(value = "_lastUpdated", required = false) String lastUpdated,
            @RequestParam(value = "patient", required = false) String patient
    ) {
        return conditionService.getConditions(id, lastUpdated, patient);
    }

    @GetMapping("/Condition/{uuid}")
    public ResponseEntity<FhirConditionResponseDTO> getConditionByUuid(
            @PathVariable String uuid
    ) {
        return conditionService.getConditionByUuid(uuid);
    }
}
