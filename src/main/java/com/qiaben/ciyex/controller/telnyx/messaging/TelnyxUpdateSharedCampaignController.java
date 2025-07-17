package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSharedCampaignDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateSharedCampaignRequestDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxUpdateSharedCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class TelnyxUpdateSharedCampaignController {

    private final TelnyxUpdateSharedCampaignService telnyxUpdateSharedCampaignService;

    @PatchMapping("/{campaignId}")
    public TelnyxSharedCampaignDTO updateSharedCampaign(
            @PathVariable String campaignId,
            @RequestBody TelnyxUpdateSharedCampaignRequestDTO requestDTO
    ) {
        return telnyxUpdateSharedCampaignService.updateCampaign(campaignId, requestDTO);
    }
}
