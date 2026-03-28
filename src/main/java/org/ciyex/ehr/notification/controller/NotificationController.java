package org.ciyex.ehr.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.notification.dto.*;
import org.ciyex.ehr.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@PreAuthorize("hasAuthority('SCOPE_user/Communication.read')")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService service;

    // ── Send ──

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<NotificationLogDto>> send(@RequestBody Map<String, Object> body) {
        try {
            String channelType = (String) body.get("channelType");
            String recipient = (String) body.get("recipient");
            String subject = (String) body.get("subject");
            String msgBody = (String) body.get("body");
            Long patientId = body.get("patientId") != null
                    ? Long.valueOf(body.get("patientId").toString()) : null;
            String triggerType = (String) body.get("triggerType");

            var result = service.send(channelType, recipient, subject, msgBody, patientId, triggerType);
            return ResponseEntity.ok(ApiResponse.ok("Notification sent", result));
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/send-template")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<NotificationLogDto>> sendFromTemplate(@RequestBody Map<String, Object> body) {
        try {
            String templateKey = (String) body.get("templateKey");
            String channelType = (String) body.get("channelType");
            String recipient = (String) body.get("recipient");
            Map<String, String> variables = (Map<String, String>) body.get("variables");
            Long patientId = body.get("patientId") != null
                    ? Long.valueOf(body.get("patientId").toString()) : null;

            var result = service.sendFromTemplate(templateKey, channelType, recipient, variables, patientId);
            return ResponseEntity.ok(ApiResponse.ok("Notification sent from template", result));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to send notification from template", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Log ──

    @GetMapping("/log")
    public ResponseEntity<ApiResponse<Page<NotificationLogDto>>> getLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            var logs = service.getLog(pageable);
            return ResponseEntity.ok(ApiResponse.ok("Notification log retrieved", logs));
        } catch (Exception e) {
            log.error("Failed to get notification log", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/log/patient/{patientId}")
    public ResponseEntity<ApiResponse<List<NotificationLogDto>>> getLogByPatient(@PathVariable Long patientId) {
        try {
            var logs = service.getLogByPatient(patientId);
            return ResponseEntity.ok(ApiResponse.ok("Patient notification log retrieved", logs));
        } catch (Exception e) {
            log.error("Failed to get notification log for patient {}", patientId, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/log/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLogStats() {
        try {
            var stats = service.getLogStats();
            return ResponseEntity.ok(ApiResponse.ok("Notification stats retrieved", stats));
        } catch (Exception e) {
            log.error("Failed to get notification stats", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    // ── Campaigns ──

    @GetMapping("/campaigns")
    public ResponseEntity<ApiResponse<List<BulkCampaignDto>>> listCampaigns() {
        try {
            var campaigns = service.listCampaigns();
            return ResponseEntity.ok(ApiResponse.ok("Campaigns retrieved", campaigns));
        } catch (Exception e) {
            log.error("Failed to list campaigns", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<ApiResponse<BulkCampaignDto>> getCampaign(@PathVariable Long id) {
        try {
            var campaign = service.getCampaign(id);
            return ResponseEntity.ok(ApiResponse.ok("Campaign retrieved", campaign));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to get campaign {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<BulkCampaignDto>> createCampaign(@RequestBody BulkCampaignDto dto) {
        try {
            var created = service.createCampaign(dto);
            return ResponseEntity.ok(ApiResponse.ok("Campaign created", created));
        } catch (Exception e) {
            log.error("Failed to create campaign", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{id}/start")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<BulkCampaignDto>> startCampaign(@PathVariable Long id) {
        try {
            var started = service.startCampaign(id);
            return ResponseEntity.ok(ApiResponse.ok("Campaign started", started));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to start campaign {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PostMapping("/campaigns/{id}/cancel")
    @PreAuthorize("hasAuthority('SCOPE_user/Communication.write')")
    public ResponseEntity<ApiResponse<BulkCampaignDto>> cancelCampaign(@PathVariable Long id) {
        try {
            var cancelled = service.cancelCampaign(id);
            return ResponseEntity.ok(ApiResponse.ok("Campaign cancelled", cancelled));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to cancel campaign {}", id, e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
