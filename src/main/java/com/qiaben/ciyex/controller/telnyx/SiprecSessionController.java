package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.SiprecSessionResponseDto;
import com.qiaben.ciyex.dto.telnyx.StartSiprecSessionRequestDto;
import com.qiaben.ciyex.service.telnyx.SiprecSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/siprec-session")
@RequiredArgsConstructor
public class SiprecSessionController {

    private final SiprecSessionService service;

    @PostMapping("/start/{accountSid}/{callSid}")
    public SiprecSessionResponseDto start(
            @PathVariable String accountSid,
            @PathVariable String callSid,
            @ModelAttribute StartSiprecSessionRequestDto dto
    ) {
        return service.startSiprecSession(accountSid, callSid, dto);
    }
}
