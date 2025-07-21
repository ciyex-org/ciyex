package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSharedCampaignDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxGetSharedCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class TelnyxGetSharedCampaignController {

    private final TelnyxGetSharedCampaignService telnyxGetSharedCampaignService;

    @GetMapping("/{campaignId}")
    public TelnyxSharedCampaignDTO getSharedCampaign(@PathVariable String campaignId) {
        return telnyxGetSharedCampaignService.getSharedCampaignById(campaignId);
    }
}
