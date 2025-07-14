package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.MediaStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/texml")
@RequiredArgsConstructor
public class MediaStreamController {

    private final MediaStreamService streamService;

    @PostMapping("/Accounts/{accountSid}/Calls/{callSid}/Stream")
    public StreamResponseDto startStream(@PathVariable String accountSid,
                                         @PathVariable String callSid,
                                         @ModelAttribute StartStreamRequestDto request) {
        return streamService.startStream(accountSid, callSid, request);
    }

    @PostMapping("/Accounts/{accountSid}/Calls/{callSid}/Stream/{streamSid}")
    public StreamResponseDto updateStream(@PathVariable String accountSid,
                                          @PathVariable String callSid,
                                          @PathVariable String streamSid,
                                          @ModelAttribute UpdateStreamRequestDto request) {
        return streamService.updateStream(accountSid, callSid, streamSid, request);
    }
}

