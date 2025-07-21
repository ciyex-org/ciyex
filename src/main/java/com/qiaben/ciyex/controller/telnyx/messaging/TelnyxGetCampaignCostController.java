package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxGetCampaignCostResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxGetCampaignCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxGetCampaignCostController {

    private final TelnyxGetCampaignCostService service;

    @GetMapping("/usecase/cost")
    public ResponseEntity<TelnyxGetCampaignCostResponseDTO> getCampaignCost(@RequestParam String usecase) {
        return ResponseEntity.ok(service.getCost(usecase));
    }
}
