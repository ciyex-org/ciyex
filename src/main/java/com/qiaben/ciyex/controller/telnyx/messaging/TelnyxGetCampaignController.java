package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxGetCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxGetCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxGetCampaignController {

    private final TelnyxGetCampaignService service;

    @GetMapping("/{campaignId}")
    public ResponseEntity<TelnyxGetCampaignResponseDTO> getCampaign(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.getCampaign(campaignId));
    }
}
