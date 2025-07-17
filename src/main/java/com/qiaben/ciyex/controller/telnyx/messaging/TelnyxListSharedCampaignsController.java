package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListSharedCampaignsResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxListSharedCampaignsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class TelnyxListSharedCampaignsController {

    private final TelnyxListSharedCampaignsService sharedCampaignsService;

    @GetMapping
    public TelnyxListSharedCampaignsResponseDTO getSharedCampaigns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int recordsPerPage,
            @RequestParam(defaultValue = "-createdAt") String sort
    ) {
        return sharedCampaignsService.listSharedCampaigns(page, recordsPerPage, sort);
    }
}
