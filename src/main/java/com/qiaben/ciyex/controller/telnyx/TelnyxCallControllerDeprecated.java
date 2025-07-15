package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.CallResponseDtoDeprecated;
import com.qiaben.ciyex.dto.telnyx.InitiateCallRequestDtoDeprecated;
import com.qiaben.ciyex.dto.telnyx.UpdateCallRequestDtoDeprecated;
import com.qiaben.ciyex.service.telnyx.TelnyxCallServiceDeprecated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/texml/calls")
@RequiredArgsConstructor
public class TelnyxCallControllerDeprecated {

    private final TelnyxCallServiceDeprecated callService;

    @PostMapping("/{applicationId}")
    public CallResponseDtoDeprecated initiateCall(
            @PathVariable String applicationId,
            @RequestBody InitiateCallRequestDtoDeprecated request
    ) {
        return callService.initiateCall(applicationId, request);
    }

    @PostMapping("/{callSid}/update")
    public CallResponseDtoDeprecated updateCall(
            @PathVariable String callSid,
            @RequestBody UpdateCallRequestDtoDeprecated request
    ) {
        return callService.updateCall(callSid, request);
    }
}

