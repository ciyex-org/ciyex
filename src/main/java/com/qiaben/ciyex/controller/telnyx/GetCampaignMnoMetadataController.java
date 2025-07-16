package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.GetCampaignMnoMetadataResponseDTO;
import com.qiaben.ciyex.service.telnyx.GetCampaignMnoMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class GetCampaignMnoMetadataController {

    private final GetCampaignMnoMetadataService service;

    @GetMapping("/{campaignId}/mno-metadata")
    public ResponseEntity<GetCampaignMnoMetadataResponseDTO> getMnoMetadata(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.getMnoMetadata(campaignId));
    }
}
