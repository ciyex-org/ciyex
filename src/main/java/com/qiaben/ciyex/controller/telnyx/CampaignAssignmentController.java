package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.CampaignAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign-assignment")
@RequiredArgsConstructor
public class CampaignAssignmentController {

    private final CampaignAssignmentService service;

    // POST  ➜ assign messaging profile to campaign
    @PostMapping
    public ResponseEntity<AssignMessagingProfileResponseDTO> assign(
            @RequestBody AssignMessagingProfileRequestDTO body) {
        return ResponseEntity
                .accepted()
                .body(service.assignMessagingProfile(body));
    }

    // GET  ➜ task status
    @GetMapping("/{taskId}")
    public ResponseEntity<TaskStatusResponseDTO> getStatus(@PathVariable String taskId) {
        return ResponseEntity.ok(service.getTaskStatus(taskId));
    }

    // GET  ➜ phone-number status list
    @GetMapping("/{taskId}/phone-number-status")
    public ResponseEntity<PhoneNumberStatusListResponseDTO> getPhoneStatus(
            @PathVariable String taskId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, name = "recordsPerPage") Integer rpp) {
        return ResponseEntity.ok(service.getPhoneNumberStatuses(taskId, page, rpp));
    }
}
