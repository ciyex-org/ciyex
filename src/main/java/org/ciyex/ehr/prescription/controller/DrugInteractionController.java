package org.ciyex.ehr.prescription.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.prescription.entity.DrugInteraction;
import org.ciyex.ehr.prescription.repository.DrugInteractionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.*;

@PreAuthorize("hasAuthority('SCOPE_user/MedicationRequest.read')")
@RestController
@RequestMapping("/api/drug-interactions")
@RequiredArgsConstructor
@Slf4j
public class DrugInteractionController {

    private final DrugInteractionRepository repo;

    /**
     * Check all pairwise drug interactions for the given drug codes.
     * GET /api/drug-interactions/check?drugs=code1,code2,code3
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> check(
            @RequestParam List<String> drugs) {
        try {
            if (drugs == null || drugs.size() < 2) {
                return ResponseEntity.ok(ApiResponse.ok("Need at least 2 drugs to check", List.of()));
            }

            String org = RequestContext.get().getOrgName();
            List<Map<String, Object>> interactions = new ArrayList<>();

            // Check all pairs
            for (int i = 0; i < drugs.size(); i++) {
                for (int j = i + 1; j < drugs.size(); j++) {
                    String codeA = drugs.get(i).trim();
                    String codeB = drugs.get(j).trim();

                    // Check both directions: A-B and B-A
                    List<DrugInteraction> found = new ArrayList<>();
                    found.addAll(repo.findInteraction(org, codeA, codeB));
                    found.addAll(repo.findInteraction(org, codeB, codeA));

                    for (DrugInteraction di : found) {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("drugACode", di.getDrugACode());
                        entry.put("drugAName", di.getDrugAName());
                        entry.put("drugBCode", di.getDrugBCode());
                        entry.put("drugBName", di.getDrugBName());
                        entry.put("severity", di.getSeverity());
                        entry.put("description", di.getDescription());
                        entry.put("clinicalEffect", di.getClinicalEffect());
                        entry.put("management", di.getManagement());
                        interactions.add(entry);
                    }
                }
            }

            String message = interactions.isEmpty()
                    ? "No interactions found"
                    : interactions.size() + " interaction(s) found";
            return ResponseEntity.ok(ApiResponse.ok(message, interactions));
        } catch (Exception e) {
            log.error("Failed to check drug interactions", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
