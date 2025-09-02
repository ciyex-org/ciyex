package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CodeTypeDto;
import com.qiaben.ciyex.service.CodeTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codetypes")
@RequiredArgsConstructor
@Slf4j
public class CodeTypeController {

    private final CodeTypeService service;

    // READ ALL by encounter
    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<CodeTypeDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {

        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<CodeTypeDto>>builder()
                .success(true).message("CodeTypes fetched").data(list).build());
    }

    // READ ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeTypeDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {

        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<CodeTypeDto>builder()
                .success(true).message("CodeType fetched").data(dto).build());
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<CodeTypeDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody CodeTypeDto dto) {

        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<CodeTypeDto>builder()
                .success(true).message("CodeType created").data(created).build());
    }

    // UPDATE
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeTypeDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody CodeTypeDto dto) {

        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<CodeTypeDto>builder()
                .success(true).message("CodeType updated").data(updated).build());
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
                .success(true).message("CodeType deleted").build());
    }

    // SEARCH (in encounter)
    @GetMapping("/{patientId}/{encounterId}/search")
    public ResponseEntity<ApiResponse<List<CodeTypeDto>>> search(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "codeTypeKey", required = false) String codeTypeKey,
            @RequestParam(value = "active", required = false) Boolean active) {

        var list = service.searchInEncounter(orgId, patientId, encounterId, codeTypeKey, active, q);
        return ResponseEntity.ok(ApiResponse.<List<CodeTypeDto>>builder()
                .success(true).message("CodeTypes search results").data(list).build());
    }
}
