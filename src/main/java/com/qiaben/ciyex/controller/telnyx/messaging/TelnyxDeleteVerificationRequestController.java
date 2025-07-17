package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.service.telnyx.messaging.TelnyxDeleteVerificationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verification-requests")
@RequiredArgsConstructor
public class TelnyxDeleteVerificationRequestController {

    private final TelnyxDeleteVerificationRequestService service;

    @DeleteMapping("/{id}")
    public String deleteVerificationRequest(@PathVariable String id) {
        return service.deleteVerificationRequest(id);
    }
}
