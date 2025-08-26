package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.HistoryOfPresentIllnessDto;
import com.qiaben.ciyex.service.HistoryOfPresentIllnessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/history-of-present-illness")
@RequiredArgsConstructor
@Slf4j
public class HistoryOfPresentIllnessController {

    private final HistoryOfPresentIllnessService service;

    // READ ALL: /api/history-of-present-illness/{patientId}
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                .success(true).message("HPI fetched successfully").data(list).build());
    }

    // READ ALL: /api/history-of-present-illness/{patientId}/{encounterId}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<HistoryOfPresentIllnessDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<HistoryOfPresentIllnessDto>>builder()
                .success(true).message("HPI fetched successfully").data(list).build());
    }

    // READ ONE: /api/history-of-present-illness/{patientId}/{encounterId}/{id}
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                .success(true).message("HPI fetched successfully").data(dto).build());
    }

    // CREATE: /api/history-of-present-illness/{patientId}/{encounterId}
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody HistoryOfPresentIllnessDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                .success(true).message("HPI created").data(created).build());
    }

    // UPDATE: /api/history-of-present-illness/{patientId}/{encounterId}/{id}
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<HistoryOfPresentIllnessDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody HistoryOfPresentIllnessDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<HistoryOfPresentIllnessDto>builder()
                .success(true).message("HPI updated").data(updated).build());
    }

    // DELETE: /api/history-of-present-illness/{patientId}/{encounterId}/{id}
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("HPI deleted").build());
    }
}
