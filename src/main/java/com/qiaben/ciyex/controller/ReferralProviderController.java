package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ReferralProviderDto;
import com.qiaben.ciyex.service.ReferralProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/referral-providers")
@Slf4j
public class ReferralProviderController {

    private final ReferralProviderService service;

    public ReferralProviderController(ReferralProviderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReferralProviderDto>> create(@RequestBody ReferralProviderDto dto) {
        try {
            ReferralProviderDto createdDto = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<ReferralProviderDto>builder()
                    .success(true)
                    .message("Referral provider created successfully")
                    .data(createdDto)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.status(400).body(ApiResponse.<ReferralProviderDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Failed to create referral provider", e);
            return ResponseEntity.status(500).body(ApiResponse.<ReferralProviderDto>builder()
                    .success(false)
                    .message("Failed to create referral provider: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReferralProviderDto>> get(@PathVariable("id") String id) {
        try {
            ReferralProviderDto dto = service.getById(id);
            return ResponseEntity.ok(ApiResponse.<ReferralProviderDto>builder()
                    .success(true)
                    .message("Referral provider retrieved successfully")
                    .data(dto)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Referral provider not found: {}", id);
            return ResponseEntity.status(404).body(ApiResponse.<ReferralProviderDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve referral provider with id {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.<ReferralProviderDto>builder()
                    .success(false)
                    .message("Failed to retrieve referral provider: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}/with-practice")
    public ResponseEntity<ApiResponse<ReferralProviderDto>> getWithPractice(@PathVariable("id") String id) {
        try {
            ReferralProviderDto dto = service.getByIdWithPractice(id);
            return ResponseEntity.ok(ApiResponse.<ReferralProviderDto>builder()
                    .success(true)
                    .message("Referral provider retrieved successfully")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve referral provider with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<ReferralProviderDto>builder()
                    .success(false)
                    .message("Failed to retrieve referral provider: " + e.getMessage())
                    .build());
        }
    }

    // NEW: list providers by practiceId (each item includes practice {id, name})
    @GetMapping("/by-practice/{practiceId}")
    public ResponseEntity<ApiResponse<List<ReferralProviderDto>>> getByPractice(@PathVariable("practiceId") Long practiceId) {
        try {
            List<ReferralProviderDto> dtoList = service.getByPracticeId(practiceId);
            return ResponseEntity.ok(ApiResponse.<List<ReferralProviderDto>>builder()
                    .success(true)
                    .message("Referral providers retrieved successfully")
                    .data(dtoList)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve referral providers for practice {}", practiceId, e);
            return ResponseEntity.ok(ApiResponse.<List<ReferralProviderDto>>builder()
                    .success(false)
                    .message("Failed to retrieve referral providers: " + e.getMessage())
                    .build());
        }
    }

    // Practice name/address endpoints moved to ReferralPracticeController

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReferralProviderDto>> update(@PathVariable("id") String id, @RequestBody ReferralProviderDto dto) {
        try {
            ReferralProviderDto updatedDto = service.update(id, dto);
            return ResponseEntity.ok(ApiResponse.<ReferralProviderDto>builder()
                    .success(true)
                    .message("Referral provider updated successfully")
                    .data(updatedDto)
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Referral provider not found: {}", id);
            return ResponseEntity.status(404).body(ApiResponse.<ReferralProviderDto>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("Failed to update referral provider with id {}", id, e);
            return ResponseEntity.status(500).body(ApiResponse.<ReferralProviderDto>builder()
                    .success(false)
                    .message("Failed to update referral provider: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable("id") String id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Referral provider deleted successfully")
                    .data(null)
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete referral provider with id {}", id, e);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete referral provider: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReferralProviderDto>>> getAll() {
        try {
            List<ReferralProviderDto> dtoList = service.getAll();
            return ResponseEntity.ok(ApiResponse.<List<ReferralProviderDto>>builder()
                    .success(true)
                    .message("Referral providers retrieved successfully")
                    .data(dtoList)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve referral providers", e);
            return ResponseEntity.ok(ApiResponse.<List<ReferralProviderDto>>builder()
                    .success(false)
                    .message("Failed to retrieve referral providers: " + e.getMessage())
                    .build());
        }
    }

}
