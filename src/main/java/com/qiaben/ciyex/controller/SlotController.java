package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.SlotDto;
import com.qiaben.ciyex.dto.SlotScheduleDto;
import com.qiaben.ciyex.service.SlotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
@Slf4j
public class SlotController {

    private final SlotService service;

    @Autowired
    public SlotController(SlotService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SlotDto>> create(@RequestBody SlotDto dto) {
        try {
            SlotDto createdSlot = service.create(dto);
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(true)
                    .message("Slot created successfully")
                    .data(createdSlot)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create slot: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(false)
                    .message("Failed to create slot: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SlotDto>> get(@PathVariable Long id) {
        try {
            SlotDto slot = service.getById(id);
            if (slot == null) {
                return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                        .success(false)
                        .message("Slot not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(true)
                    .message("Slot retrieved successfully")
                    .data(slot)
                    .build());
        } catch (Exception e) {
            log.error("Failed to retrieve slot with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(false)
                    .message("Failed to retrieve slot: " + e.getMessage())
                    .build());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SlotDto>> update(@PathVariable Long id, @RequestBody SlotDto dto) {
        try {
            SlotDto updatedSlot = service.update(id, dto);
            if (updatedSlot == null) {
                return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                        .success(false)
                        .message("Slot not found with id: " + id)
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(true)
                    .message("Slot updated successfully")
                    .data(updatedSlot)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update slot with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<SlotDto>builder()
                    .success(false)
                    .message("Failed to update slot: " + e.getMessage())
                    .build());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Slot deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete slot with id {}: {}", id, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete slot: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SlotDto>>> getAllSlots() {
        try {
            ApiResponse<List<SlotDto>> response = service.getAllSlots();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to retrieve all slots: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<SlotDto>>builder()
                    .success(false)
                    .message("Failed to retrieve slots: " + e.getMessage())
                    .build());
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Void>> generateSlots(@RequestBody SlotScheduleDto scheduleDto) {
        try {
            service.generateProviderSlots(scheduleDto);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Slots generated successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate slots: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to generate slots: " + e.getMessage())
                    .build());
        }
    }
}