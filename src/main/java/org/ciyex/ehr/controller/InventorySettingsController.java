package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.inventory.dto.InvSettingsDto2;
import org.ciyex.ehr.inventory.service.InvSettingsService2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/inventory-settings")
@RequiredArgsConstructor
@Slf4j
public class InventorySettingsController {

    private final InvSettingsService2 settingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<InvSettingsDto2>> get() {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Settings retrieved", settingsService.getSettings()));
        } catch (Exception e) {
            log.error("Failed to get inventory settings", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to retrieve settings: " + e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvSettingsDto2>> create(@RequestBody InvSettingsDto2 dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Settings saved", settingsService.saveSettings(dto)));
        } catch (Exception e) {
            log.error("Failed to save inventory settings", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to save settings: " + e.getMessage()));
        }
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<InvSettingsDto2>> update(@RequestBody InvSettingsDto2 dto) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Settings updated", settingsService.saveSettings(dto)));
        } catch (Exception e) {
            log.error("Failed to update inventory settings", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to update settings: " + e.getMessage()));
        }
    }
}
