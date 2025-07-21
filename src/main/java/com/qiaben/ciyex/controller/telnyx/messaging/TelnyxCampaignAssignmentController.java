package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxAssignMessagingProfileRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxAssignMessagingProfileResponseDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxPhoneNumberStatusListResponseDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTaskStatusResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxCampaignAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign-assignment")
@RequiredArgsConstructor
public class TelnyxCampaignAssignmentController {

    private final TelnyxCampaignAssignmentService service;

    // POST  ➜ assign messaging profile to campaign
    @PostMapping
    public ResponseEntity<TelnyxAssignMessagingProfileResponseDTO> assign(
            @RequestBody TelnyxAssignMessagingProfileRequestDTO body) {
        return ResponseEntity
                .accepted()
                .body(service.assignMessagingProfile(body));
    }

    // GET  ➜ task status
    @GetMapping("/{taskId}")
    public ResponseEntity<TelnyxTaskStatusResponseDTO> getStatus(@PathVariable String taskId) {
        return ResponseEntity.ok(service.getTaskStatus(taskId));
    }

    // GET  ➜ phone-number status list
    @GetMapping("/{taskId}/phone-number-status")
    public ResponseEntity<TelnyxPhoneNumberStatusListResponseDTO> getPhoneStatus(
            @PathVariable String taskId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, name = "recordsPerPage") Integer rpp) {
        return ResponseEntity.ok(service.getPhoneNumberStatuses(taskId, page, rpp));
    }
}
