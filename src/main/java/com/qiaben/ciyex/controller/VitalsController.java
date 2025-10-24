

package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.VitalsDto;
import com.qiaben.ciyex.service.VitalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vitals")
@RequiredArgsConstructor
public class VitalsController {
    private final VitalsService service;

    @PostMapping("/{patientId}/{encounterId}")
    public ApiResponse<VitalsDto> create(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @RequestBody VitalsDto dto) {
        return ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals recorded")
                .data(service.create(patientId, encounterId, dto))
                .build();
    }

    @GetMapping("/{patientId}/{encounterId}/{id}")
    public ApiResponse<VitalsDto> get(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        return ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals retrieved")
                .data(service.get(patientId, encounterId, id))
                .build();
    }

    @GetMapping("/{patientId}/{encounterId}")
    public ApiResponse<List<VitalsDto>> getByEncounter(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long encounterId) {
        return ApiResponse.<List<VitalsDto>>builder()
                .success(true)
                .message("Vitals by encounter")
                .data(service.getByEncounter(orgId, patientId, encounterId))
                .build();
    }

    @PutMapping("/{patientId}/{encounterId}/{id}")
    public ApiResponse<VitalsDto> update(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id,
            @RequestBody VitalsDto dto) {
        return ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals updated")
                .data(service.update(orgId, patientId, encounterId, id, dto))
                .build();
    }

    @DeleteMapping("/{patientId}/{encounterId}/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        service.delete(orgId, patientId, encounterId, id);
        return ApiResponse.<Void>builder()
                .success(true)
                .message("Vitals deleted")
                .build();
    }

    @PostMapping("/{patientId}/{encounterId}/{id}/esign")
    public ApiResponse<VitalsDto> eSign(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        return ApiResponse.<VitalsDto>builder()
                .success(true)
                .message("Vitals signed")
                .data(service.eSign(orgId, patientId, encounterId, id))
                .build();
    }

    @GetMapping("/{patientId}/{encounterId}/{id}/print")
    public ResponseEntity<byte[]> print(
            @RequestHeader("orgId") Long orgId,
            @PathVariable Long patientId,
            @PathVariable Long encounterId,
            @PathVariable Long id) {
        byte[] pdf = service.print(orgId, patientId, encounterId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=vitals-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
