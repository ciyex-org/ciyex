package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCallResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateCallRequestDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxUpdateCallService;  // ← corrected package
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/texml")
@RequiredArgsConstructor
public class TelnyxUpdateCallController {

    private final TelnyxUpdateCallService telnyxUpdateCallService;

    @PostMapping("/Accounts/{accountSid}/Calls/{callSid}")
    public TelnyxCallResponseDto updateCall(@PathVariable String accountSid,
                                            @PathVariable String callSid,
                                            @ModelAttribute TelnyxUpdateCallRequestDto request) {
        return telnyxUpdateCallService.updateCall(accountSid, callSid, request);
    }
}
