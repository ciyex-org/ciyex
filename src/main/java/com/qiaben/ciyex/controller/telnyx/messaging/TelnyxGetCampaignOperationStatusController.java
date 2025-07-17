package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxGetCampaignOperationStatusResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxGetCampaignOperationStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxGetCampaignOperationStatusController {

    private final TelnyxGetCampaignOperationStatusService service;

    @GetMapping("/{campaignId}/operation-status")
    public ResponseEntity<TelnyxGetCampaignOperationStatusResponseDTO> getOperationStatus(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.getStatus(campaignId));
    }
}
