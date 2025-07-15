package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.PartnerCampaignSharedByMeResponseDTO;
import com.qiaben.ciyex.service.telnyx.GetPartnerCampaignsSharedByMeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class GetPartnerCampaignsSharedByMeController {

    private final GetPartnerCampaignsSharedByMeService service;

    @GetMapping("/shared-by-me")
    public PartnerCampaignSharedByMeResponseDTO getSharedByMe(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int recordsPerPage
    ) {
        return service.getCampaigns(page, recordsPerPage);
    }
}
