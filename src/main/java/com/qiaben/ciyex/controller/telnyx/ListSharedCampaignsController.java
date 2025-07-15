package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ListSharedCampaignsResponseDTO;
import com.qiaben.ciyex.service.telnyx.ListSharedCampaignsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/shared-campaigns")
@RequiredArgsConstructor
public class ListSharedCampaignsController {

    private final ListSharedCampaignsService sharedCampaignsService;

    @GetMapping
    public ListSharedCampaignsResponseDTO getSharedCampaigns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int recordsPerPage,
            @RequestParam(defaultValue = "-createdAt") String sort
    ) {
        return sharedCampaignsService.listSharedCampaigns(page, recordsPerPage, sort);
    }
}
