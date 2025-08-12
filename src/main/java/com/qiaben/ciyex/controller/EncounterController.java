package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.service.EncounterService;
import com.qiaben.ciyex.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/encounters")
public class EncounterController {

    private final EncounterService encounterService;

    @Autowired
    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EncounterDto>> createEncounter(@RequestBody EncounterDto encounterDto) {
        try {
            EncounterDto createdEncounter = encounterService.createEncounter(encounterDto);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter created successfully")
                    .data(createdEncounter)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Failed to create encounter: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EncounterDto>> getEncounterById(@PathVariable Long id) {
        try {
            EncounterDto encounter = encounterService.getEncounterById(id);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter fetched successfully")
                    .data(encounter)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Encounter not found: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EncounterDto>> updateEncounter(@PathVariable Long id, @RequestBody EncounterDto encounterDto) {
        try {
            EncounterDto updatedEncounter = encounterService.updateEncounter(id, encounterDto);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter updated successfully")
                    .data(updatedEncounter)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Failed to update encounter: " + e.getMessage())
                    .build());
        }
    }
}
