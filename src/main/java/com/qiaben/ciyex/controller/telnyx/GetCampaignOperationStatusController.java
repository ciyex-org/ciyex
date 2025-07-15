package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.GetCampaignOperationStatusResponseDTO;
import com.qiaben.ciyex.service.telnyx.GetCampaignOperationStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class GetCampaignOperationStatusController {

    private final GetCampaignOperationStatusService service;

    @GetMapping("/{campaignId}/operation-status")
    public ResponseEntity<GetCampaignOperationStatusResponseDTO> getOperationStatus(@PathVariable String campaignId) {
        return ResponseEntity.ok(service.getStatus(campaignId));
    }
}
