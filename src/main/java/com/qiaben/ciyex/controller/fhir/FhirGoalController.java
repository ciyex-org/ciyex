package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.FhirGoalDTO;
import com.qiaben.ciyex.service.fhir.FhirGoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/fhir/Goal")
@RequiredArgsConstructor
public class FhirGoalController {

    private final FhirGoalService goalService;

    // Endpoint to fetch all Goal resources
    @GetMapping
    public ResponseEntity<FhirGoalDTO> getGoals(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated,
            @RequestParam(required = false) String patient
    ) {
        Map<String, String> queryParams = Map.of(
                "_id", _id,
                "_lastUpdated", _lastUpdated,
                "patient", patient
        );

        FhirGoalDTO goalDTO = goalService.getGoals(queryParams);
        return ResponseEntity.ok(goalDTO);
    }

    // Endpoint to fetch a single Goal by UUID
    @GetMapping("/{uuid}")
    public ResponseEntity<FhirGoalDTO> getGoalByUuid(@PathVariable String uuid) {
        FhirGoalDTO goalDTO = goalService.getGoalByUuid(uuid);
        if (goalDTO == null) {
            return ResponseEntity.status(404).build();  // Not Found
        }
        return ResponseEntity.ok(goalDTO);
    }
}

