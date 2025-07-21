package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.HostedNumberEligibilityRequestDto;
import com.qiaben.ciyex.dto.telnyx.HostedNumberEligibilityResponseDto;
import com.qiaben.ciyex.service.telnyx.HostedNumberEligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/hosted-number")
@RequiredArgsConstructor
public class HostedNumberEligibilityController {

    private final HostedNumberEligibilityService service;

    /** POST /api/telnyx/hosted-number/eligibility */
    @PostMapping("/eligibility")
    public ResponseEntity<HostedNumberEligibilityResponseDto> checkEligibility(
            @RequestBody HostedNumberEligibilityRequestDto request) {

        HostedNumberEligibilityResponseDto resp = service.checkEligibility(request);
        return ResponseEntity.ok(resp);
    }
}
