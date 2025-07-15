package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.AcceptSharedCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.AcceptSharedCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class AcceptSharedCampaignController {

    private final AcceptSharedCampaignService service;

    @PostMapping("/accept/{campaignId}")
    public ResponseEntity<AcceptSharedCampaignResponseDTO> acceptSharedCampaign(
            @PathVariable String campaignId) {
        return ResponseEntity.ok(service.accept(campaignId));
    }
}
