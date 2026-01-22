package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ReferralPracticeDto;
import com.qiaben.ciyex.service.ReferralPracticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referral-practices")
@Slf4j
public class ReferralPracticeController {

    private final ReferralPracticeService service;

    public ReferralPracticeController(ReferralPracticeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReferralPracticeDto>> create(@RequestBody ReferralPracticeDto dto) {
        try {
            ReferralPracticeDto createdDto = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<ReferralPracticeDto>builder()
                    .success(true)
                    .message("Referral practice created successfully")
                    .data(createdDto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create referral practice", e);
            return ResponseEntity.ok(ApiResponse.<ReferralPracticeDto>builder()
                    .success(false)
                    .message("Failed to create referral practice: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReferralPracticeDto>> get(@PathVariable String id) {
        try {
            ReferralPracticeDto dto = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<ReferralPracticeDto>builder()
                    .success(true)
                    .message("Referral practice retrieved successfully")
                    .data(dto)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Referral practice not found: {}", id);
            return ResponseEntity.status(404).body(ApiResponse.<ReferralPracticeDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve referral practice with id {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.<ReferralPracticeDto>builder()
                    .success(false)
                    .message("Failed to retrieve referral practice: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReferralPracticeDto>> update(@PathVariable String id, @RequestBody ReferralPracticeDto dto) {
        try {
            ReferralPracticeDto updatedDto = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<ReferralPracticeDto>builder()
                    .success(true)
                    .message("Referral practice updated successfully")
                    .data(updatedDto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update referral practice with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<ReferralPracticeDto>builder()
                    .success(false)
                    .message("Failed to update referral practice: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Referral practice deleted successfully")
                    .data(null)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete referral practice with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete referral practice: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReferralPracticeDto>>> getAll() {
        try {
            List<ReferralPracticeDto> dtoList = service.getAll();
            return ResponseEntity.ok(ApiResponse.<List<ReferralPracticeDto>>builder()
                    .success(true)
                    .message("Referral practices retrieved successfully")
                    .data(dtoList)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve referral practices", e);
            return ResponseEntity.ok(ApiResponse.<List<ReferralPracticeDto>>builder()
                    .success(false)
                    .message("Failed to retrieve referral practices: " + e.getMessage())
                    .build());
        }
    }
}
