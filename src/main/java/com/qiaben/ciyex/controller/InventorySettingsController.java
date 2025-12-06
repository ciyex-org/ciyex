package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.InventorySettingsDto;
import com.qiaben.ciyex.service.InventorySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory-settings")
@RequiredArgsConstructor
@Slf4j
public class InventorySettingsController {

    private final InventorySettingsService service;

    /** ✅ Get settings (returns defaults if none exist) */
    @GetMapping
    public ResponseEntity<ApiResponse<InventorySettingsDto>> get() {
        try {
            InventorySettingsDto dto = service.getSettings();
            return ResponseEntity.ok(
                    ApiResponse.<InventorySettingsDto>builder()
                            .success(true)
                            .message("Inventory settings retrieved successfully")
                            .data(dto)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get inventory settings", e);
            return ResponseEntity.ok(
                    ApiResponse.<InventorySettingsDto>builder()
                            .success(false)
                            .message("Failed to retrieve inventory settings: " + e.getMessage())
                            .build()
            );
        }
    }

    /** ✅ Create or update settings */
    @PostMapping
    public ResponseEntity<ApiResponse<InventorySettingsDto>> create(
            @RequestBody InventorySettingsDto dto) {
        try {
            InventorySettingsDto updated = service.updateSettings(dto);
            return ResponseEntity.ok(
                    ApiResponse.<InventorySettingsDto>builder()
                            .success(true)
                            .message("Inventory settings created successfully")
                            .data(updated)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to create inventory settings", e);
            return ResponseEntity.ok(
                    ApiResponse.<InventorySettingsDto>builder()
                            .success(false)
                            .message("Failed to create inventory settings: " + e.getMessage())
                            .build()
            );
        }
    }

    /** ✅ Update settings */
    @PutMapping
    public ResponseEntity<ApiResponse<InventorySettingsDto>> update(
            @RequestBody InventorySettingsDto dto) {
        try {
            InventorySettingsDto updated = service.updateSettings(dto);
            return ResponseEntity.ok(
                    ApiResponse.<InventorySettingsDto>builder()
                            .success(true)
                            .message("Inventory settings updated successfully")
                            .data(updated)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to update inventory settings", e);
            return ResponseEntity.ok(
                    ApiResponse.<InventorySettingsDto>builder()
                            .success(false)
                            .message("Failed to update inventory settings: " + e.getMessage())
                            .build()
            );
        }
    }
}
