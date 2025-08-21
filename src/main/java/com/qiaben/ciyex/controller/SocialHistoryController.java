package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SocialHistoryDto;
import com.qiaben.ciyex.service.SocialHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social-history")
@RequiredArgsConstructor
@Slf4j
public class SocialHistoryController {

    private final SocialHistoryService service;

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<SocialHistoryDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<SocialHistoryDto>>builder()
                .success(true).message("Social History fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<SocialHistoryDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<SocialHistoryDto>>builder()
                .success(true).message("Social History fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SocialHistoryDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
                .success(true).message("Social History fetched").data(dto).build());
    }

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<SocialHistoryDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody SocialHistoryDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
                .success(true).message("Social History created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SocialHistoryDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody SocialHistoryDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<SocialHistoryDto>builder()
                .success(true).message("Social History updated").data(updated).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Social History deleted").build());
    }
}
