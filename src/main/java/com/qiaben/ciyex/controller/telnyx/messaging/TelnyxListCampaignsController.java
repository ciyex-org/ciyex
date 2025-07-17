package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListCampaignsResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxListCampaignsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaigns")
@RequiredArgsConstructor
public class TelnyxListCampaignsController {

    private final TelnyxListCampaignsService service;

    @GetMapping
    public ResponseEntity<TelnyxListCampaignsResponseDTO> getCampaigns(
            @RequestParam String brandId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer recordsPerPage,
            @RequestParam(defaultValue = "-createdAt") String sort) {

        return ResponseEntity.ok(
                service.listCampaigns(brandId, page, recordsPerPage, sort)
        );
    }
}
