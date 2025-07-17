package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSiprecSessionResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxStartSiprecSessionRequestDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxSiprecSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/siprec-session")
@RequiredArgsConstructor
public class TelnyxSiprecSessionController {

    private final TelnyxSiprecSessionService service;

    @PostMapping("/start/{accountSid}/{callSid}")
    public TelnyxSiprecSessionResponseDto start(
            @PathVariable String accountSid,
            @PathVariable String callSid,
            @ModelAttribute TelnyxStartSiprecSessionRequestDto dto
    ) {
        return service.startSiprecSession(accountSid, callSid, dto);
    }
}
