package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.FhirCareTeamDTO;
import com.qiaben.ciyex.service.fhir.FhirCareTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fhir/CareTeam")
@RequiredArgsConstructor
public class FhirCareTeamController {

    private final FhirCareTeamService careTeamService;

    // Endpoint to fetch all CareTeam resources
    @GetMapping
    public ResponseEntity<List<FhirCareTeamDTO>> getCareTeams(
            @RequestParam(required = false) String _id,
            @RequestParam(required = false) String _lastUpdated,
            @RequestParam(required = false) String patient,
            @RequestParam(required = false) String status
    ) {
        Map<String, String> queryParams = Map.of(
                "_id", _id,
                "_lastUpdated", _lastUpdated,
                "patient", patient,
                "status", status
        );

        List<FhirCareTeamDTO> careTeams = careTeamService.getCareTeams(queryParams);
        return ResponseEntity.ok(careTeams);
    }

    // Endpoint to fetch a specific CareTeam by UUID
    @GetMapping("/{uuid}")
    public ResponseEntity<FhirCareTeamDTO> getCareTeamByUuid(@PathVariable String uuid) {
        FhirCareTeamDTO careTeam = careTeamService.getCareTeamByUuid(uuid);
        if (careTeam == null) {
            return ResponseEntity.status(404).build();  // Not Found
        }
        return ResponseEntity.ok(careTeam);
    }
}

