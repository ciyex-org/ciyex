package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.SharedCampaignDTO;
import com.qiaben.ciyex.service.telnyx.GetSharedCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class GetSharedCampaignController {

    private final GetSharedCampaignService getSharedCampaignService;

    @GetMapping("/{campaignId}")
    public SharedCampaignDTO getSharedCampaign(@PathVariable String campaignId) {
        return getSharedCampaignService.getSharedCampaignById(campaignId);
    }
}
