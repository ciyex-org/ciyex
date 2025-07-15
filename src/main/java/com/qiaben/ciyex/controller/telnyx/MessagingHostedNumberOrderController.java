package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.HostedNumberEligibilityRequestDto;
import com.qiaben.ciyex.dto.telnyx.HostedNumberEligibilityResponseDto;

import com.qiaben.ciyex.service.telnyx.MessagingHostedNumberOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/messaging-hosted-number")
@RequiredArgsConstructor
public class MessagingHostedNumberOrderController {

    private final MessagingHostedNumberOrderService service;

    @PostMapping("/eligibility")
    public ResponseEntity<HostedNumberEligibilityResponseDto> checkEligibility(
            @RequestBody HostedNumberEligibilityRequestDto request) {

        HostedNumberEligibilityResponseDto resp = service.checkEligibility(request);
        return ResponseEntity.ok(resp);
    }
}
