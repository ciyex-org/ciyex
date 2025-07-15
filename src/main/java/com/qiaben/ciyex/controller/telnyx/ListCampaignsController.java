package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ListCampaignsResponseDTO;
import com.qiaben.ciyex.service.telnyx.ListCampaignsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaigns")
@RequiredArgsConstructor
public class ListCampaignsController {

    private final ListCampaignsService service;

    @GetMapping
    public ResponseEntity<ListCampaignsResponseDTO> getCampaigns(
            @RequestParam String brandId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer recordsPerPage,
            @RequestParam(defaultValue = "-createdAt") String sort) {

        return ResponseEntity.ok(
                service.listCampaigns(brandId, page, recordsPerPage, sort)
        );
    }
}
