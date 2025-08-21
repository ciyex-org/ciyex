package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SignoffDto;
import com.qiaben.ciyex.service.SignoffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signoffs")
@RequiredArgsConstructor
@Slf4j
public class SignoffController {

    private final SignoffService service;

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<SignoffDto>>> getAllByPatient(
            @PathVariable Long patientId, @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<SignoffDto>>builder()
                .success(true).message("Signoffs fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<SignoffDto>>> getAllByEncounter(
            @PathVariable Long patientId, @PathVariable Long encounterId, @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<SignoffDto>>builder()
                .success(true).message("Signoffs fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SignoffDto>> getOne(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
                .success(true).message("Signoff fetched").data(dto).build());
    }

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<SignoffDto>> create(
            @PathVariable Long patientId, @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody SignoffDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
                .success(true).message("Signoff created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<SignoffDto>> update(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody SignoffDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<SignoffDto>builder()
                .success(true).message("Signoff updated").data(updated).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId, @PathVariable Long encounterId, @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Signoff deleted").build());
    }
}
