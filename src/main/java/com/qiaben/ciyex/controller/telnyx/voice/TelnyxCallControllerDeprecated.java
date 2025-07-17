package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCallResponseDtoDeprecated;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxInitiateCallRequestDtoDeprecated;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateCallRequestDtoDeprecated;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCallServiceDeprecated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/texml/calls")
@RequiredArgsConstructor
public class TelnyxCallControllerDeprecated {

    private final TelnyxCallServiceDeprecated callService;

    @PostMapping("/{applicationId}")
    public TelnyxCallResponseDtoDeprecated initiateCall(
            @PathVariable String applicationId,
            @RequestBody TelnyxInitiateCallRequestDtoDeprecated request
    ) {
        return callService.initiateCall(applicationId, request);
    }

    @PostMapping("/{callSid}/update")
    public TelnyxCallResponseDtoDeprecated updateCall(
            @PathVariable String callSid,
            @RequestBody TelnyxUpdateCallRequestDtoDeprecated request
    ) {
        return callService.updateCall(callSid, request);
    }
}

