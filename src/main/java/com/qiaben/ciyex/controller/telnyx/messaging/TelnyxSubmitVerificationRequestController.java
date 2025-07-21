package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSubmitVerificationRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSubmitVerificationResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxSubmitVerificationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verification-requests")
@RequiredArgsConstructor
public class TelnyxSubmitVerificationRequestController {

    private final TelnyxSubmitVerificationRequestService service;

    @PostMapping
    public TelnyxSubmitVerificationResponseDTO submitVerificationRequest(
            @RequestBody TelnyxSubmitVerificationRequestDTO dto
    ) {
        return service.submitRequest(dto);
    }
}
