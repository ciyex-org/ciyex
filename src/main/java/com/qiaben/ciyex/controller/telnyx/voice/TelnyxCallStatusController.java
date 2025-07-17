package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCallStatusResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCallStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls")
@RequiredArgsConstructor
public class TelnyxCallStatusController {

    private final TelnyxCallStatusService telnyxCallStatusService;

    @GetMapping("/{callControlId}")
    public TelnyxCallStatusResponseDto getCallStatus(@PathVariable String callControlId) {
        return telnyxCallStatusService.getCallStatus(callControlId);
    }
}
