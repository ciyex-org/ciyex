package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.SharedCampaignDTO;
import com.qiaben.ciyex.dto.telnyx.UpdateSharedCampaignRequestDTO;
import com.qiaben.ciyex.service.telnyx.UpdateSharedCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class UpdateSharedCampaignController {

    private final UpdateSharedCampaignService updateSharedCampaignService;

    @PatchMapping("/{campaignId}")
    public SharedCampaignDTO updateSharedCampaign(
            @PathVariable String campaignId,
            @RequestBody UpdateSharedCampaignRequestDTO requestDTO
    ) {
        return updateSharedCampaignService.updateCampaign(campaignId, requestDTO);
    }
}
