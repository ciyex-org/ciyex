package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxStartStreamRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxStreamResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateStreamRequestDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxMediaStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/texml")
@RequiredArgsConstructor
public class TelnyxMediaStreamController {

    private final TelnyxMediaStreamService streamService;

    @PostMapping("/Accounts/{accountSid}/Calls/{callSid}/Stream")
    public TelnyxStreamResponseDto startStream(@PathVariable String accountSid,
                                               @PathVariable String callSid,
                                               @ModelAttribute TelnyxStartStreamRequestDto request) {
        return streamService.startStream(accountSid, callSid, request);
    }

    @PostMapping("/Accounts/{accountSid}/Calls/{callSid}/Stream/{streamSid}")
    public TelnyxStreamResponseDto updateStream(@PathVariable String accountSid,
                                                @PathVariable String callSid,
                                                @PathVariable String streamSid,
                                                @ModelAttribute TelnyxUpdateStreamRequestDto request) {
        return streamService.updateStream(accountSid, callSid, streamSid, request);
    }
}

