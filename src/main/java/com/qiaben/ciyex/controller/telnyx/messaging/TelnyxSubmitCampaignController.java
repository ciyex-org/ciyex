package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSubmitCampaignRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSubmitCampaignResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxSubmitCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxSubmitCampaignController {

    private final TelnyxSubmitCampaignService service;

    @PostMapping("/submit")
    public ResponseEntity<TelnyxSubmitCampaignResponseDTO> submit(@RequestBody TelnyxSubmitCampaignRequestDTO request) {
        return ResponseEntity.ok(service.submitCampaign(request));
    }
}
