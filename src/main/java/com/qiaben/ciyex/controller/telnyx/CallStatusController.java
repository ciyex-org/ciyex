package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.CallStatusResponseDto;
import com.qiaben.ciyex.service.telnyx.CallStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls")
@RequiredArgsConstructor
public class CallStatusController {

    private final CallStatusService callStatusService;

    @GetMapping("/{callControlId}")
    public CallStatusResponseDto getCallStatus(@PathVariable String callControlId) {
        return callStatusService.getCallStatus(callControlId);
    }
}
