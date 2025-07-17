package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxActiveCallListResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxActiveCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/connections")
@RequiredArgsConstructor
public class TelnyxActiveCallController {

    private final TelnyxActiveCallService telnyxActiveCallService;

    @GetMapping("/{connectionId}/active_calls")
    public TelnyxActiveCallListResponseDto getActiveCalls(
            @PathVariable String connectionId,
            @RequestParam Map<String, String> queryParams) {
        return telnyxActiveCallService.getActiveCalls(connectionId, queryParams);
    }
}
