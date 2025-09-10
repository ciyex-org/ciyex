package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.CodeDto;
import com.qiaben.ciyex.service.CodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
@Slf4j
public class CodeController {

    private final CodeService service;

    // ✅ Get all codes by org
    @GetMapping
    public ResponseEntity<ApiResponse<List<CodeDto>>> getAll(
            @RequestHeader("orgId") Long orgId) {
        var list = service.getAll(orgId);
        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
                .success(true).message("Codes fetched").data(list).build());
    }

    // ✅ Get one by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CodeDto>> getOne(
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        var dto = service.getOne(orgId, id);
        return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                .success(true).message("Code fetched").data(dto).build());
    }

    // ✅ Create (with validation)
    @PostMapping
    public ResponseEntity<ApiResponse<CodeDto>> create(
            @RequestHeader("orgId") Long orgId,
            @Valid @RequestBody CodeDto dto) {   // 👈 @Valid ensures @NotBlank is checked
        var created = service.create(orgId, dto);
        return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                .success(true).message("Code created").data(created).build());
    }

    // ✅ Update (with validation)
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CodeDto>> update(
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId,
            @Valid @RequestBody CodeDto dto) {   // 👈 @Valid here too
        var updated = service.update(orgId, id, dto);
        return ResponseEntity.ok(ApiResponse.<CodeDto>builder()
                .success(true).message("Code updated").data(updated).build());
    }

    // ✅ Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @RequestHeader("orgId") Long orgId) {
        service.delete(orgId, id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Code deleted").build());
    }

    // ✅ Filter by type
    @GetMapping("/type/{codeType}")
    public ResponseEntity<ApiResponse<List<CodeDto>>> listByType(
            @PathVariable String codeType,
            @RequestHeader("orgId") Long orgId,
            @RequestParam(value = "active", required = false) Boolean active) {
        var list = service.search(orgId, codeType, active, "");
        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
                .success(true).message("Codes filtered").data(list).build());
    }

    // ✅ Search
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CodeDto>>> search(
            @RequestHeader("orgId") Long orgId,
            @RequestParam(value = "q", required = false, defaultValue = "") String q,
            @RequestParam(value = "codeType", required = false) String codeType,
            @RequestParam(value = "active", required = false) Boolean active) {
        var list = service.search(orgId, codeType, active, q);
        return ResponseEntity.ok(ApiResponse.<List<CodeDto>>builder()
                .success(true).message("Codes search results").data(list).build());
    }
}
