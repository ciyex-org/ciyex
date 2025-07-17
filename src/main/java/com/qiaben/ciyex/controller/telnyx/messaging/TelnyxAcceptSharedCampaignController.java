package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxAcceptSharedCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxAcceptSharedCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxAcceptSharedCampaignController {

    private final TelnyxAcceptSharedCampaignService service;

    @PostMapping("/accept/{campaignId}")
    public ResponseEntity<TelnyxAcceptSharedCampaignResponseDTO> acceptSharedCampaign(
            @PathVariable String campaignId) {
        return ResponseEntity.ok(service.accept(campaignId));
    }
}
