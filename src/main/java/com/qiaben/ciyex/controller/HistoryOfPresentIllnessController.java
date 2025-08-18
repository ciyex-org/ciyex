//package com.qiaben.ciyex.controller;
//
//import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
//import com.qiaben.ciyex.service.HistoryOfPresentIllnessService;
//import com.qiaben.ciyex.dto.ApiResponse;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/{encounterId}/history-of-present-illness")  // Dynamic encounterId in the URL path
//public class HistoryOfPresentIllnessController {
//
//    private final HistoryOfPresentIllnessService historyOfPresentIllnessService;
//
//    public HistoryOfPresentIllnessController(HistoryOfPresentIllnessService historyOfPresentIllnessService) {
//        this.historyOfPresentIllnessService = historyOfPresentIllnessService;
//    }
//
//    // Create a new History of Present Illness entry
//    @PostMapping
//    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> create(
//            @PathVariable Long encounterId,
//            @RequestBody HistoryOfPresentIllnessDto dto,
//            @RequestHeader("orgid") Long orgId  // Pass orgId in the header
//    ) {
//        dto.setOrgId(orgId);  // Set orgId from header
//        dto.setEncounterId(encounterId);  // Set encounterId from URL path
//        HistoryOfPresentIllnessDto createdHPI = historyOfPresentIllnessService.create(dto);
//        ApiResponse<HistoryOfPresentIllnessDto> response = new ApiResponse.Builder<HistoryOfPresentIllnessDto>()
//                .success(true)
//                .message("History of Present Illness created successfully")
//                .data(createdHPI)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    // Get all History of Present Illness entries for a specific encounter
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> getAll(@PathVariable Long encounterId) {
//        List<HistoryOfPresentIllnessDto> hpis = historyOfPresentIllnessService.getByEncounterId(encounterId);
//        ApiResponse<List<HistoryOfPresentIllnessDto>> response = new ApiResponse.Builder<List<HistoryOfPresentIllnessDto>>()
//                .success(true)
//                .message("History of Present Illness entries fetched successfully")
//                .data(hpis)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    // Update a specific History of Present Illness entry
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> update(
//            @PathVariable Long encounterId,
//            @PathVariable Long id,
//            @RequestBody HistoryOfPresentIllnessDto dto
//    ) {
//        HistoryOfPresentIllnessDto updatedHPI = historyOfPresentIllnessService.update(encounterId, id, dto);
//        ApiResponse<HistoryOfPresentIllnessDto> response = new ApiResponse.Builder<HistoryOfPresentIllnessDto>()
//                .success(true)
//                .message("History of Present Illness updated successfully")
//                .data(updatedHPI)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//
//    // Delete a specific History of Present Illness entry
//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long encounterId, @PathVariable Long id) {
//        historyOfPresentIllnessService.delete(encounterId, id);
//        ApiResponse<Void> response = new ApiResponse.Builder<Void>()
//                .success(true)
//                .message("History of Present Illness deleted successfully")
//                .data(null)
//                .build();
//        return ResponseEntity.ok(response);
//    }
//}

package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
import com.qiaben.ciyex.service.HistoryOfPresentIllnessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NOTE: Path uses the exact string you’re calling: "historyofpresentlllness" (3 Ls).
 */
@RestController
@RequestMapping("/api/historyofpresentlllness")
@RequiredArgsConstructor
@Slf4j
public class HistoryOfPresentIllnessController {

    private final HistoryOfPresentIllnessService service;

    // GET /api/historyofpresentlllness/{patientId}
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        List<HistoryOfPresentIllnessDto> list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                .success(true).message("HPI fetched successfully").data(list).build());
    }

    // GET /api/historyofpresentlllness/{patientId}/{encounterId}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> getAllByPatientAndEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        List<HistoryOfPresentIllnessDto> list = service.getAllByPatientAndEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                .success(true).message("HPI fetched successfully").data(list).build());
    }

    // GET /api/historyofpresentlllness/{patientId}/{encounterId}/{hpiId}
    @GetMapping("/{patientId}/{encounterId}/{hpiId}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> getById(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long hpiId,
            @RequestHeader("orgId") Long orgId) {
        HistoryOfPresentIllnessDto dto = service.getById(orgId, patientId, encounterId, hpiId);
        return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                .success(true).message("HPI fetched successfully").data(dto).build());
    }

    // POST /api/historyofpresentlllness/{patientId}/{encounterId}
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody HistoryOfPresentIllnessDto dto) {
        HistoryOfPresentIllnessDto created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                .success(true).message("HPI created successfully").data(created).build());
    }

    // PUT /api/historyofpresentlllness/{patientId}/{encounterId}/{hpiId}
    @PutMapping("/{patientId}/{encounterId}/{hpiId}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long hpiId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody HistoryOfPresentIllnessDto dto) {
        HistoryOfPresentIllnessDto updated = service.update(orgId, patientId, encounterId, hpiId, dto);
        return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                .success(true).message("HPI updated successfully").data(updated).build());
    }

    // DELETE /api/historyofpresentlllness/{patientId}/{encounterId}/{hpiId}
    @DeleteMapping("/{patientId}/{encounterId}/{hpiId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long hpiId,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, hpiId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("HPI deleted successfully").build());
    }
}
