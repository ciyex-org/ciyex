package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.RecallDto;
import com.qiaben.ciyex.service.RecallService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recalls")
public class RecallController {

    private final RecallService service;

    public RecallController(RecallService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecallDto>> create(@RequestBody RecallDto dto) {
        RecallDto created = service.create(dto);
        return ResponseEntity.ok(
                ApiResponse.<RecallDto>builder()
                        .success(true)
                        .message("Recall created successfully")
                        .data(created)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecallDto>> get(@PathVariable Long id) {
        RecallDto recall = service.getById(id);
        return ResponseEntity.ok(
                ApiResponse.<RecallDto>builder()
                        .success(true)
                        .message("Recall retrieved successfully")
                        .data(recall)
                        .build()
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecallDto>> update(@PathVariable Long id, @RequestBody RecallDto dto) {
        RecallDto updated = service.update(id, dto);
        return ResponseEntity.ok(
                ApiResponse.<RecallDto>builder()
                        .success(true)
                        .message("Recall updated successfully")
                        .data(updated)
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .success(true)
                        .message("Recall deleted successfully")
                        .data(null)
                        .build()
        );
    }

    // ✅ Paginated recalls
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecallDto>>> getAll(@PageableDefault Pageable pageable) {
        Page<RecallDto> recalls = service.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.<Page<RecallDto>>builder()
                .success(true)
                .message("Recalls retrieved successfully")
                .data(recalls)
                .build());
    }
}
