package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.RecallDto;
import com.qiaben.ciyex.service.RecallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recalls")
@RequiredArgsConstructor
@Slf4j
public class RecallController {


    private final RecallService service;

    /** ✅ Create a new Recall */
    @PostMapping
    public ResponseEntity<ApiResponse<RecallDto>> create(@RequestBody RecallDto dto) {
        try {
            RecallDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<RecallDto>builder()
                    .success(true)
                    .message("Recall created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create recall", e);
            return ResponseEntity.ok(ApiResponse.<RecallDto>builder()
                    .success(false)
                    .message("Failed to create recall: " + e.getMessage())
                    .build());
        }
    }

    /** ✅ Retrieve a Recall by ID */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecallDto>> get(@PathVariable String id) {
        try {
            RecallDto recall = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<RecallDto>builder()
                    .success(true)
                    .message("Recall retrieved successfully")
                    .data(recall)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve recall with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<RecallDto>builder()
                    .success(false)
                    .message("Failed to retrieve recall: " + e.getMessage())
                    .build());
        }
    }

    /** ✅ Update a Recall by ID */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecallDto>> update(@PathVariable String id, @RequestBody RecallDto dto) {
        try {
            RecallDto updated = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<RecallDto>builder()
                    .success(true)
                    .message("Recall updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update recall with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<RecallDto>builder()
                    .success(false)
                    .message("Failed to update recall: " + e.getMessage())
                    .build());
        }
    }

    /** ✅ Delete a Recall by ID */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Recall deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete recall with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete recall: " + e.getMessage())
                    .build());
        }
    }

    /** ✅ Paginated list of Recalls */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecallDto>>> getAll(@PageableDefault Pageable pageable) {
        try {
            Page<RecallDto> recalls = service.getAll(pageable);
            return ResponseEntity.ok(ApiResponse.<Page<RecallDto>>builder()
                    .success(true)
                    .message("Recalls retrieved successfully")
                    .data(recalls)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve recalls", e);
            return ResponseEntity.ok(ApiResponse.<Page<RecallDto>>builder()
                    .success(false)
                    .message("Failed to retrieve recalls: " + e.getMessage())
                    .build());
        }
    }


}
 