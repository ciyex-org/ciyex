package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxMessagingVerificationRequestDTO;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxMessagingVerificationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verification-requests")
@RequiredArgsConstructor
public class TelnyxMessagingVerificationRequestController {

    private final TelnyxMessagingVerificationRequestService service;

    @GetMapping
    public TelnyxMessagingVerificationRequestDTO getVerificationRequests(
            @RequestParam int page,
            @RequestParam(name = "page_size") int pageSize,
            @RequestParam(required = false) String date_start,
            @RequestParam(required = false) String date_end,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String phone_number
    ) {
        return service.listVerificationRequests(page, pageSize, date_start, date_end, status, phone_number);
    }
}
