package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.DeactivateCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.DeactivateCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class DeactivateCampaignController {

    private final DeactivateCampaignService service;

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<DeactivateCampaignResponseDTO> deactivateCampaign(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.deactivateCampaign(campaignId));
    }
}
