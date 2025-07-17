package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxHostedNumberEligibilityRequestDto;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxHostedNumberEligibilityResponseDto;

import com.qiaben.ciyex.service.telnyx.messaging.TelnyxMessagingHostedNumberOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/messaging-hosted-number")
@RequiredArgsConstructor
public class TelnyxMessagingHostedNumberOrderController {

    private final TelnyxMessagingHostedNumberOrderService service;

    @PostMapping("/eligibility")
    public ResponseEntity<TelnyxHostedNumberEligibilityResponseDto> checkEligibility(
            @RequestBody TelnyxHostedNumberEligibilityRequestDto request) {

        TelnyxHostedNumberEligibilityResponseDto resp = service.checkEligibility(request);
        return ResponseEntity.ok(resp);
    }
}
