package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.FamilyHistoryDto;
import com.qiaben.ciyex.service.FamilyHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-history")
@RequiredArgsConstructor
@Slf4j
public class FamilyHistoryController {

    private final FamilyHistoryService service;

    // READ ALL: /api/family-history/{patientId}
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<FamilyHistoryDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<FamilyHistoryDto>>builder()
                .success(true).message("Family History fetched").data(list).build());
    }

    // READ ALL: /api/family-history/{patientId}/{encounterId}
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<FamilyHistoryDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<FamilyHistoryDto>>builder()
                .success(true).message("Family History fetched").data(list).build());
    }

    // READ ONE: /api/family-history/{patientId}/{encounterId}/{id}
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<FamilyHistoryDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
                .success(true).message("Family History fetched").data(dto).build());
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<FamilyHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody FamilyHistoryDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
                .success(true).message("Family History created").data(created).build());
    }

    // UPDATE (replace entries)
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<FamilyHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody FamilyHistoryDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<FamilyHistoryDto>builder()
                .success(true).message("Family History updated").data(updated).build());
    }

    // DELETE
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Family History deleted").build());
    }
}
