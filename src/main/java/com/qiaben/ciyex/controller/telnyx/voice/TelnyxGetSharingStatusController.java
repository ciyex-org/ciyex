package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxCampaignSharingStatusDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxGetSharingStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class TelnyxGetSharingStatusController {

    private final TelnyxGetSharingStatusService sharingStatusService;

    @GetMapping("/{campaignId}/sharing-status")
    public TelnyxCampaignSharingStatusDTO getSharingStatus(@PathVariable String campaignId) {
        return sharingStatusService.getSharingStatus(campaignId);
    }
}
