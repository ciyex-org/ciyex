




package com.qiaben.ciyex.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.service.EncounterService;

@RestController
@RequestMapping("/api/{patientId}/encounters")

public class EncounterController {

    private final EncounterService encounterService;

    @Autowired
    public EncounterController(EncounterService encounterService) {
        this.encounterService = encounterService;
    }




    // CREATE
    @PostMapping
    public ResponseEntity<ApiResponse<EncounterDto>> createEncounter(
            @PathVariable Long patientId,
            @RequestBody EncounterDto encounterDto,
            @RequestHeader("orgId") Long orgId
    ) {
        try {
            // enforce patient scope from path
            encounterDto.setPatientId(patientId);
            EncounterDto created = encounterService.createEncounter(patientId, encounterDto, orgId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter created")
                    .data(created)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Failed to create encounter: " + e.getMessage())
                    .build());
        }
    }

    // LIST for patient
    @GetMapping
    public ResponseEntity<ApiResponse<List<EncounterDto>>> listEncounters(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId
    ) {
        try {
            List<EncounterDto> items = encounterService.listByPatient(patientId, orgId);
            return ResponseEntity.ok(ApiResponse.<List<EncounterDto>>builder()
                    .success(true)
                    .message("Encounters fetched")
                    .data(items)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.<List<EncounterDto>>builder()
                    .success(false)
                    .message("Failed to list encounters: " + e.getMessage())
                    .build());
        }
    }

    // GET one by id (scoped)
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EncounterDto>> getEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId
    ) {
        try {
            EncounterDto dto = encounterService.getByIdForPatient(id, patientId, orgId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter fetched")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Encounter not found: " + e.getMessage())
                    .build());
        }
    }

    // UPDATE (scoped)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EncounterDto>> updateEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id,
            @RequestBody EncounterDto encounterDto,
            @RequestHeader("orgId") Long orgId
    ) {
        try {
            encounterDto.setPatientId(patientId);
            EncounterDto updated = encounterService.updateEncounter(id, patientId, encounterDto, orgId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter updated")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Failed to update encounter: " + e.getMessage())
                    .build());
        }
    }

    // DELETE (scoped)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId
    ) {
        try {
            encounterService.deleteEncounter(id, patientId, orgId);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Encounter deleted")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(404).body(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete encounter: " + e.getMessage())
                    .build());
        }
    }
    @PostMapping("/{id}/sign")
    public ResponseEntity<ApiResponse<EncounterDto>> signEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId
    ) {
        try {
            EncounterDto dto = encounterService.signEncounter(id, patientId, orgId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter signed")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Failed to sign encounter: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/unsign")
    public ResponseEntity<ApiResponse<EncounterDto>> unsignEncounter(
            @PathVariable Long patientId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId
    ) {
        try {
            EncounterDto dto = encounterService.unsignEncounter(id, patientId, orgId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter unsigned")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Failed to unsign encounter: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/{id}/incomplete")
    public ResponseEntity<ApiResponse<EncounterDto>> markIncomplete(
            @PathVariable Long patientId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId
    ) {
        try {
            EncounterDto dto = encounterService.markIncomplete(id, patientId, orgId);
            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
                    .success(true)
                    .message("Encounter marked incomplete")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.<EncounterDto>builder()
                    .success(false)
                    .message("Failed to mark encounter incomplete: " + e.getMessage())
                    .build());
        }
    }
    
    

}



