package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.InventorySettingsDto;
import com.qiaben.ciyex.service.InventorySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory-settings")
@RequiredArgsConstructor
public class InventorySettingsController {

    private final InventorySettingsService service;

    /** ✅ Get settings for an org */
    @GetMapping("/{orgId}")
    public ResponseEntity<ApiResponse<InventorySettingsDto>> get(@PathVariable Long orgId) {
        InventorySettingsDto dto = service.getSettings();
        return ResponseEntity.ok(
                ApiResponse.<InventorySettingsDto>builder()
                        .success(true)
                        .message("Inventory settings retrieved successfully")
                        .data(dto)
                        .build()
        );
    }

    /** ✅ Update settings for an org */
    @PutMapping("/{orgId}")
    public ResponseEntity<ApiResponse<InventorySettingsDto>> update(
            @RequestBody InventorySettingsDto dto) {
        InventorySettingsDto updated = service.updateSettings(dto);
        return ResponseEntity.ok(
                ApiResponse.<InventorySettingsDto>builder()
                        .success(true)
                        .message("Inventory settings updated successfully")
                        .data(updated)
                        .build()
        );
    }
}
