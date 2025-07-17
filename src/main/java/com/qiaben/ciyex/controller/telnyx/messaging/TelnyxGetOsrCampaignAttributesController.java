package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxGetOsrCampaignAttributesResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxGetOsrCampaignAttributesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxGetOsrCampaignAttributesController {

    private final TelnyxGetOsrCampaignAttributesService service;

    @GetMapping("/{campaignId}/osr/attributes")
    public ResponseEntity<TelnyxGetOsrCampaignAttributesResponseDTO> getOsrAttributes(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.getAttributes(campaignId));
    }
}
