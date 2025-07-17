package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxPartnerCampaignSharedByMeResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxGetPartnerCampaignsSharedByMeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class TelnyxGetPartnerCampaignsSharedByMeController {

    private final TelnyxGetPartnerCampaignsSharedByMeService service;

    @GetMapping("/shared-by-me")
    public TelnyxPartnerCampaignSharedByMeResponseDTO getSharedByMe(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int recordsPerPage
    ) {
        return service.getCampaigns(page, recordsPerPage);
    }
}
