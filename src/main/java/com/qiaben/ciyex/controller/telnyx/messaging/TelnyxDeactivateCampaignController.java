package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxDeactivateCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxDeactivateCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxDeactivateCampaignController {

    private final TelnyxDeactivateCampaignService service;

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<TelnyxDeactivateCampaignResponseDTO> deactivateCampaign(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.deactivateCampaign(campaignId));
    }
}
