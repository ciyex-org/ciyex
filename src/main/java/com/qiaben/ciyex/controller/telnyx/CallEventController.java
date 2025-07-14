package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.CallEventListResponseDto;
import com.qiaben.ciyex.service.telnyx.CallEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/call-events")
@RequiredArgsConstructor
public class CallEventController {

    private final CallEventService callEventService;

    @GetMapping
    public CallEventListResponseDto listCallEvents(@RequestParam Map<String, String> queryParams) {
        return callEventService.listCallEvents(queryParams);
    }
}
