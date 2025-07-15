package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.CampaignSharingStatusDTO;
import com.qiaben.ciyex.service.telnyx.GetSharingStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class GetSharingStatusController {

    private final GetSharingStatusService sharingStatusService;

    @GetMapping("/{campaignId}/sharing-status")
    public CampaignSharingStatusDTO getSharingStatus(@PathVariable String campaignId) {
        return sharingStatusService.getSharingStatus(campaignId);
    }
}
