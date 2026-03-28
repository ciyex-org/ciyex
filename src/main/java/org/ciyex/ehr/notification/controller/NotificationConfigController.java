package org.ciyex.ehr.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.notification.dto.*;
import org.ciyex.ehr.notification.service.NotificationConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/notifications/config")
@RequiredArgsConstructor
@Slf4j
public class NotificationConfigController {

    private final NotificationConfigService service;

    // ── Provider Config ──

    @GetMapping("/{channelType}")
    public ResponseEntity<ApiResponse<NotificationConfigDto>> getConfig(@PathVariable String channelType) {
        try {
            var config = service.getConfig(channelType);
            return ResponseEntity.ok(ApiResponse.ok("Config retrieved", config));
        } catch (Exception e) {
            log.error("Failed to get notification config for channel {}", channelType, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{channelType}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<NotificationConfigDto>> saveConfig(
            @PathVariable String channelType, @RequestBody NotificationConfigDto dto) {
        try {
            var saved = service.saveConfig(channelType, dto);
            return ResponseEntity.ok(ApiResponse.ok("Config saved", saved));
        } catch (Exception e) {
            log.error("Failed to save notification config for channel {}", channelType, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{channelType}/test")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConfig(@PathVariable String channelType) {
        try {
            var result = service.testConfig(channelType);
            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                return ResponseEntity.ok(ApiResponse.ok("Connection test passed", result));
            } else {
                return ResponseEntity.ok(ApiResponse.error((String) result.get("message")));
            }
        } catch (Exception e) {
            log.error("Failed to test notification config for channel {}", channelType, e);
            return ResponseEntity.ok(ApiResponse.error("Test failed: " + e.getMessage()));
        }
    }

    // ── Templates ──

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<NotificationTemplateDto>>> listTemplates() {
        try {
            var templates = service.listTemplates();
            return ResponseEntity.ok(ApiResponse.ok("Templates retrieved", templates));
        } catch (Exception e) {
            log.error("Failed to list notification templates", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<NotificationTemplateDto>> getTemplate(@PathVariable Long id) {
        try {
            var template = service.getTemplate(id);
            return ResponseEntity.ok(ApiResponse.ok("Template retrieved", template));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get notification template {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<NotificationTemplateDto>> createTemplate(
            @RequestBody NotificationTemplateDto dto) {
        try {
            var created = service.createTemplate(dto);
            return ResponseEntity.ok(ApiResponse.ok("Template created", created));
        } catch (Exception e) {
            log.error("Failed to create notification template", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<NotificationTemplateDto>> updateTemplate(
            @PathVariable Long id, @RequestBody NotificationTemplateDto dto) {
        try {
            var updated = service.updateTemplate(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Template updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update notification template {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        try {
            service.deleteTemplate(id);
            return ResponseEntity.ok(ApiResponse.ok("Template deleted", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete notification template {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Event Preferences ──

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceDto>>> listPreferences() {
        try {
            var prefs = service.listPreferences();
            return ResponseEntity.ok(ApiResponse.ok("Preferences retrieved", prefs));
        } catch (Exception e) {
            log.error("Failed to list notification preferences", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/preferences")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<List<NotificationPreferenceDto>>> savePreferences(
            @RequestBody List<NotificationPreferenceDto> dtos) {
        try {
            var saved = dtos.stream().map(service::savePreference).toList();
            return ResponseEntity.ok(ApiResponse.ok("Preferences saved", saved));
        } catch (Exception e) {
            log.error("Failed to save notification preferences", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Patient Communication Preferences ──

    @GetMapping("/patient-preferences/{patientId}")
    public ResponseEntity<ApiResponse<PatientCommPreferenceDto>> getPatientPreference(
            @PathVariable Long patientId) {
        try {
            var pref = service.getPatientPreference(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient preference retrieved", pref));
        } catch (Exception e) {
            log.error("Failed to get patient comm preference for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping("/patient-preferences/{patientId}")
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<PatientCommPreferenceDto>> savePatientPreference(
            @PathVariable Long patientId, @RequestBody PatientCommPreferenceDto dto) {
        try {
            var saved = service.savePatientPreference(patientId, dto);
            return ResponseEntity.ok(ApiResponse.ok("Patient preference saved", saved));
        } catch (Exception e) {
            log.error("Failed to save patient comm preference for patient {}", patientId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
