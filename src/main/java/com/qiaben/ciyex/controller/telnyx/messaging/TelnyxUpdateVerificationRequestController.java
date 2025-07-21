package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateVerificationRequestDTO;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateVerificationResponseDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxUpdateVerificationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verification-requests")
@RequiredArgsConstructor
public class TelnyxUpdateVerificationRequestController {

    private final TelnyxUpdateVerificationRequestService service;

    @PatchMapping("/{id}")
    public TelnyxUpdateVerificationResponseDTO updateRequest(
            @PathVariable String id,
            @RequestBody TelnyxUpdateVerificationRequestDTO dto
    ) {
        return service.updateRequest(id, dto);
    }
}
