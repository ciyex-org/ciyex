package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxGetVerificationResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxGetVerificationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verification-requests")
@RequiredArgsConstructor
public class TelnyxGetVerificationRequestController {

    private final TelnyxGetVerificationRequestService service;

    @GetMapping("/{id}")
    public TelnyxGetVerificationResponseDTO getVerificationById(@PathVariable String id) {
        return service.getVerificationRequestById(id);
    }
}
