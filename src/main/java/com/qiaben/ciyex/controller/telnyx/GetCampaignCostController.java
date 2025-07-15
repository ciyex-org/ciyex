package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.GetCampaignCostResponseDTO;
import com.qiaben.ciyex.service.telnyx.GetCampaignCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class GetCampaignCostController {

    private final GetCampaignCostService service;

    @GetMapping("/usecase/cost")
    public ResponseEntity<GetCampaignCostResponseDTO> getCampaignCost(@RequestParam String usecase) {
        return ResponseEntity.ok(service.getCost(usecase));
    }
}
