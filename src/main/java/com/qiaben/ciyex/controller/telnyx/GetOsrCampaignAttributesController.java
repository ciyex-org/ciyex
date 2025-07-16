package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.GetOsrCampaignAttributesResponseDTO;
import com.qiaben.ciyex.service.telnyx.GetOsrCampaignAttributesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class GetOsrCampaignAttributesController {

    private final GetOsrCampaignAttributesService service;

    @GetMapping("/{campaignId}/osr/attributes")
    public ResponseEntity<GetOsrCampaignAttributesResponseDTO> getOsrAttributes(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.getAttributes(campaignId));
    }
}
