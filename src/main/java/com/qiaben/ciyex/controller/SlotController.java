package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SlotDto;
import com.qiaben.ciyex.service.SlotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
@Slf4j
public class SlotController {

    private final SlotService service;

    public SlotController(SlotService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SlotDto>> create(@RequestBody SlotDto dto) {
        try {
            SlotDto created = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(true)
                    .message("Slot created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Create slot failed: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(false)
                    .message("Create slot failed: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SlotDto>> get(@PathVariable String id) {
        try {
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(true)
                    .message("Slot retrieved successfully")
                    .data(service.getById(id))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(false)
                    .message("Get slot failed: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SlotDto>> update(@PathVariable String id, @RequestBody SlotDto dto) {
        try {
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(true)
                    .message("Slot updated successfully")
                    .data(service.update(id, dto))
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(false)
                    .message("Update slot failed: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Slot deleted successfully")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Delete slot failed: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SlotDto>>> getAll() {
        try {
            return ResponseEntity.ok(service.getAllSlots());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<List<SlotDto>>builder()
                    .success(false)
                    .message("Get all slots failed: " + e.getMessage())
                    .build());
        }
    }

}
