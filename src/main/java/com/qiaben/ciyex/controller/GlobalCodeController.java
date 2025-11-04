package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.GlobalCodeDto;
import com.qiaben.ciyex.service.GlobalCodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codess")
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
            @PathVariable Long id){
        var dto = service.getOne(id);
        return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                .success(true).message("Code fetched").data(dto).build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GlobalCodeDto>> create(
            @Valid @RequestBody GlobalCodeDto dto) {
        var created = service.create(dto);
        return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                .success(true).message("Code created").data(created).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GlobalCodeDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody GlobalCodeDto dto) {
        var updated = service.update( id, dto);
        return ResponseEntity.ok(ApiResponse.<GlobalCodeDto>builder()
                .success(true).message("Code updated").data(updated).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id) {
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
