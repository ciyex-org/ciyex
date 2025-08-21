package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.PhysicalExamDto;
import com.qiaben.ciyex.service.PhysicalExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/physical-exam")
@RequiredArgsConstructor
@Slf4j
public class PhysicalExamController {

    private final PhysicalExamService service;

    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<PhysicalExamDto>>> getAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByPatient(orgId, patientId);
        return ResponseEntity.ok(ApiResponse.<List<PhysicalExamDto>>builder()
                .success(true).message("Physical Exam fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<List<PhysicalExamDto>>> getAllByEncounter(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAllByEncounter(orgId, patientId, encounterId);
        return ResponseEntity.ok(ApiResponse.<List<PhysicalExamDto>>builder()
                .success(true).message("Physical Exam fetched").data(list).build());
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PhysicalExamDto>> getOne(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<PhysicalExamDto>builder()
                .success(true).message("Physical Exam fetched").data(dto).build());
    }

    @PostMapping("/{patientId}/{encounterId}")
    public ResponseEntity<ApiResponse<PhysicalExamDto>> create(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PhysicalExamDto dto) {
        var created = service.create(orgId, patientId, encounterId, dto);
        return ResponseEntity.ok(ApiResponse.<PhysicalExamDto>builder()
                .success(true).message("Physical Exam created").data(created).build());
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<PhysicalExamDto>> update(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @RequestBody PhysicalExamDto dto) {
        var updated = service.update(orgId, patientId, encounterId, id, dto);
        return ResponseEntity.ok(ApiResponse.<PhysicalExamDto>builder()
                .success(true).message("Physical Exam updated").data(updated).build());
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, patientId, encounterId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Physical Exam deleted").build());
    }
}
