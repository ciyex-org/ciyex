package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateCampaignRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxUpdateCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxUpdateCampaignController {

    private final TelnyxUpdateCampaignService service;

    @PutMapping("/{campaignId}")
    public ResponseEntity<TelnyxUpdateCampaignResponseDTO> updateCampaign(
            @PathVariable String campaignId,
            @RequestBody TelnyxUpdateCampaignRequestDTO request) {
        return ResponseEntity.ok(service.updateCampaign(campaignId, request));
    }
}
