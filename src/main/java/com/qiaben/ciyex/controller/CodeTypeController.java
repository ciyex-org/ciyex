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
            @PathVariable Long encounterId) {

        var list = service.getAllByEncounter(patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<CodeTypeDto>>builder()
                .success(true).message("CodeTypes fetched").data(list).build());
    }

    // READ ONE
    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeTypeDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {

        var dto = service.getOne(patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<CodeTypeDto>builder()
                .success(true).message("CodeType fetched").data(dto).build());
    }

    // CREATE
    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<CodeTypeDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody CodeTypeDto dto) {

        var created = service.create(patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<CodeTypeDto>builder()
                .success(true).message("CodeType created").data(created).build());
    }

    // UPDATE
    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<CodeTypeDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody CodeTypeDto dto) {

        var updated = service.update(patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<CodeTypeDto>builder()
                .success(true).message("CodeType updated").data(updated).build());
    }

    // DELETE
    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {

        service.delete(patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("CodeType deleted").build());
    }

    // SEARCH (in encounter)
    @GetMapping("/{patientId}/{encounterId}/search")
    public ResponseEntity<ApiResponse<List<CodeTypeDto>>> search(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "codeTypeKey", required = false) String codeTypeKey,
            @RequestParam(value = "active", required = false) Boolean active) {

        var list = service.searchInEncounter(patientId, encounterId, codeTypeKey, active, q);
        return ResponseEntity.ok(ApiResponse.<List<CodeTypeDto>>builder()
                .success(true).message("CodeTypes search results").data(list).build());
    }
}
