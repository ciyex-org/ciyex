package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxGetCampaignMnoMetadataResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxGetCampaignMnoMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxGetCampaignMnoMetadataController {

    private final TelnyxGetCampaignMnoMetadataService service;

    @GetMapping("/{campaignId}/mno-metadata")
    public ResponseEntity<TelnyxGetCampaignMnoMetadataResponseDTO> getMnoMetadata(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.getMnoMetadata(campaignId));
    }
}
