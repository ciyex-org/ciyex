package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.UpdateCampaignRequestDTO;
import com.qiaben.ciyex.dto.telnyx.UpdateCampaignRequestDTO;
import com.qiaben.ciyex.dto.telnyx.UpdateCampaignResponseDTO;
import com.qiaben.ciyex.dto.telnyx.UpdateCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.UpdateCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class UpdateCampaignController {

    private final UpdateCampaignService service;

    @PutMapping("/{campaignId}")
    public ResponseEntity<UpdateCampaignResponseDTO> updateCampaign(
            @PathVariable String campaignId,
            @RequestBody UpdateCampaignRequestDTO request) {
        return ResponseEntity.ok(service.updateCampaign(campaignId, request));
    }
}
