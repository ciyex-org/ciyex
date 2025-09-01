//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.EncounterDto;
//import com.qiaben.ciyex.service.EncounterService;
//import com.qiaben.ciyex.dto.ApiResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/encounters")
//public class EncounterController {
//
//    private final EncounterService encounterService;
//
//    @Autowired
//    public EncounterController(EncounterService encounterService) {
//        this.encounterService = encounterService;
//    }
//
//    @PostMapping
//    public ResponseEntity<ApiResponse<EncounterDto>> createEncounter(@RequestBody EncounterDto encounterDto) {
//        try {
//            EncounterDto createdEncounter = encounterService.createEncounter(encounterDto);
//            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
//                    .success(true)
//                    .message("Encounter created successfully")
//                    .data(createdEncounter)
//                    .build());
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
//                    .success(false)
//                    .message("Failed to create encounter: " + e.getMessage())
//                    .build());
//        }
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<EncounterDto>> getEncounterById(@PathVariable Long id) {
//        try {
//            EncounterDto encounter = encounterService.getEncounterById(id);
//            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
//                    .success(true)
//                    .message("Encounter fetched successfully")
//                    .data(encounter)
//                    .build());
//        } catch (Exception e) {
//            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
//                    .success(false)
//                    .message("Encounter not found: " + e.getMessage())
//                    .build());
//        }
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<EncounterDto>> updateEncounter(@PathVariable Long id, @RequestBody EncounterDto encounterDto) {
//        try {
//            EncounterDto updatedEncounter = encounterService.updateEncounter(id, encounterDto);
//            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
//                    .success(true)
//                    .message("Encounter updated successfully")
//                    .data(updatedEncounter)
//                    .build());
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
//                    .success(false)
//                    .message("Failed to update encounter: " + e.getMessage())
//                    .build());
//        }
//    }
//}

//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.ApiResponse;
//import com.qiaben.ciyex.dto.EncounterDto;
//import com.qiaben.ciyex.service.EncounterService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/encounters")
//public class EncounterController {
//
//    private final EncounterService encounterService;
//
//    @Autowired
//    public EncounterController(EncounterService encounterService) {
//        this.encounterService = encounterService;
//    }
//
//    // Create Encounter
//    @PostMapping
//    public ResponseEntity<ApiResponse<EncounterDto>> createEncounter(@RequestBody EncounterDto encounterDto,
//                                                                     @RequestHeader(value = "orgId") Long orgId) {
//        try {
//            EncounterDto createdEncounter = encounterService.createEncounter(encounterDto, orgId);
//            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
//                    .success(true)
//                    .message("Encounter created successfully")
//                    .data(createdEncounter)
//                    .build());
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
//                    .success(false)
//                    .message("Failed to create encounter: " + e.getMessage())
//                    .build());
//        }
//    }
//
//    // Get Encounter by Id
//    @GetMapping("/{id}")
//    public ResponseEntity<ApiResponse<EncounterDto>> getEncounterById(@PathVariable Long id,
//                                                                      @RequestHeader(value = "orgId") Long orgId) {
//        try {
//            EncounterDto encounter = encounterService.getEncounterById(id, orgId);
//            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
//                    .success(true)
//                    .message("Encounter fetched successfully")
//                    .data(encounter)
//                    .build());
//        } catch (Exception e) {
//            return ResponseEntity.status(404).body(ApiResponse.<EncounterDto>builder()
//                    .success(false)
//                    .message("Encounter not found: " + e.getMessage())
//                    .build());
//        }
//    }
//
//    // Update Encounter by Id
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<EncounterDto>> updateEncounter(@PathVariable Long id,
//                                                                     @RequestBody EncounterDto encounterDto,
//                                                                     @RequestHeader(value = "orgId") Long orgId) {
//        try {
//            EncounterDto updatedEncounter = encounterService.updateEncounter(id, encounterDto, orgId);
//            return ResponseEntity.ok(ApiResponse.<EncounterDto>builder()
//                    .success(true)
//                    .message("Encounter updated successfully")
//                    .data(updatedEncounter)
//                    .build());
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(ApiResponse.<EncounterDto>builder()
//                    .success(false)
//                    .message("Failed to update encounter: " + e.getMessage())
//                    .build());
//        }
//    }
//}


package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.EncounterDto;
import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.EncounterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}

