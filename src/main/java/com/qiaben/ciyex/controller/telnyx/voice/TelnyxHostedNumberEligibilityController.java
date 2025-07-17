package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxHostedNumberEligibilityRequestDto;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxHostedNumberEligibilityResponseDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxHostedNumberEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/hosted-number")
@RequiredArgsConstructor
public class TelnyxHostedNumberEligibilityController {

    private final TelnyxHostedNumberEligibilityService service;

    /** POST /api/telnyx/hosted-number/eligibility */
    @PostMapping("/eligibility")
    public ResponseEntity<TelnyxHostedNumberEligibilityResponseDto> checkEligibility(
            @RequestBody TelnyxHostedNumberEligibilityRequestDto request) {

        TelnyxHostedNumberEligibilityResponseDto resp = service.checkEligibility(request);
        return ResponseEntity.ok(resp);
    }
}
