package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.GlobalCodeDto;
import org.ciyex.ehr.service.GlobalCodeService;
import jakarta.validation.Valid;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@PreAuthorize("hasAuthority('SCOPE_user/Practitioner.read')")
@RestController
@RequestMapping("/api/global_codes")
@RequiredArgsConstructor
@Slf4j

public class GlobalCodeController {

        
    private final GlobalCodeService service;

    @GetMapping

    public ResponseEntity<ApiResponse<List<GlobalCodeDto>>> getAll() {
        var list = service.getAll();
        return ResponseEntity.ok(ApiResponse.<List<GlobalCodeDto>>builder()
                .success(true).message("Codes fetched").data(list).build());
    }



    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GlobalCodeDto>> getOne(
            @PathVariable String id){
        var dto = service.getOne(id);
        return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                .success(true).message("Code fetched").data(dto).build());
    }

        @PostMapping
        public ResponseEntity<ApiResponse<GlobalCodeDto>> create(
                        @RequestBody GlobalCodeDto dto) {
                try {
                        if (dto == null) {
                                return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                                                .success(false).message("Payload is required").build());
                        }
                        if (!StringUtils.hasText(dto.getCodeType())) {
                                return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                                                .success(false).message("codeType is required").build());
                        }
                        if (!StringUtils.hasText(dto.getCode())) {
                                return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                                                .success(false).message("code is required").build());
                        }
                        var created = service.create(dto);
                        return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()

                                        .success(true).message("Code created successfully")
                                        .data(created).build());
                } catch (Exception e) {
                        log.error("Failed to create code: {}", e.getMessage(), e);
                        return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                                        .success(false).message("Failed to create code: " + e.getMessage()).build());
                }
        }

        @PutMapping("/{id}")
        public ResponseEntity<ApiResponse<GlobalCodeDto>> update(
                        @PathVariable String id,
                        @RequestBody GlobalCodeDto dto) {
                try {
                        if (dto == null) {
                                return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                                                .success(false).message("Payload is required").build());
                        }
                        if (!StringUtils.hasText(dto.getCodeType())) {
                                return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                                                .success(false).message("codeType is required").build());
                        }
                        if (!StringUtils.hasText(dto.getCode())) {
                                return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                                                .success(false).message("code is required").build());
                        }
                        var updated = service.update(id, dto);
                        return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                                        .success(true).message("Code updated successfully")
                                        .data(updated).build());
                } catch (Exception e) {
                        log.error("Failed to update code id {}: {}", id, e.getMessage(), e);
                        return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                                        .success(false).message("Failed to update code: " + e.getMessage()).build());
                }
        }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id) {
                
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Code deleted").build());
    }

    @GetMapping("/type/{codeType}")
    public ResponseEntity<ApiResponse<List<GlobalCodeDto>>> listByType(
            @PathVariable String codeType,
            @RequestParam(value = "active", required = false) Boolean active) {
        var list = service.search(codeType, active, "");
        return ResponseEntity.ok(ApiResponse.<List<GlobalCodeDto>>builder()
                .success(true).message("Codes filtered").data(list).build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GlobalCodeDto>>> search(
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "codeType", required = false) String codeType,
            @RequestParam(value = "active", required = false) Boolean active) {
        var list = service.search(codeType, active, q);
        return ResponseEntity.ok(ApiResponse.<List<GlobalCodeDto>>builder()
                .success(true).message("Codes search results").data(list).build());
    }
}
