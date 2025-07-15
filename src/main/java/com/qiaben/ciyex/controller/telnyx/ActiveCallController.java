package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ActiveCallListResponseDto;
import com.qiaben.ciyex.service.telnyx.ActiveCallService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/connections")
@RequiredArgsConstructor
public class ActiveCallController {

    private final ActiveCallService activeCallService;

    @GetMapping("/{connectionId}/active_calls")
    public ActiveCallListResponseDto getActiveCalls(
            @PathVariable String connectionId,
            @RequestParam Map<String, String> queryParams) {
        return activeCallService.getActiveCalls(connectionId, queryParams);
    }
}
