package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCallEventListResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCallEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/call-events")
@RequiredArgsConstructor
public class TelnyxCallEventController {

    private final TelnyxCallEventService telnyxCallEventService;

    @GetMapping
    public TelnyxCallEventListResponseDto listCallEvents(@RequestParam Map<String, String> queryParams) {
        return telnyxCallEventService.listCallEvents(queryParams);
    }
}
