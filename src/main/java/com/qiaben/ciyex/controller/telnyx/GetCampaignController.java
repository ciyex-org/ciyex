package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.GetCampaignResponseDTO;
import com.qiaben.ciyex.dto.telnyx.GetCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.GetCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class GetCampaignController {

    private final GetCampaignService service;

    @GetMapping("/{campaignId}")
    public ResponseEntity<GetCampaignResponseDTO> getCampaign(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.getCampaign(campaignId));
    }
}
