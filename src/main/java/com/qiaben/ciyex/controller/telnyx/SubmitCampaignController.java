package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.SubmitCampaignRequestDTO;
import com.qiaben.ciyex.dto.telnyx.SubmitCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.SubmitCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class SubmitCampaignController {

    private final SubmitCampaignService service;

    @PostMapping("/submit")
    public ResponseEntity<SubmitCampaignResponseDTO> submit(@RequestBody SubmitCampaignRequestDTO request) {
        return ResponseEntity.ok(service.submitCampaign(request));
    }
}
