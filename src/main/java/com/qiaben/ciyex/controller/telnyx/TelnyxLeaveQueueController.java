package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxLeaveQueueRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxLeaveQueueResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxLeaveQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/call")
@RequiredArgsConstructor
public class TelnyxLeaveQueueController {

    private final TelnyxLeaveQueueService leaveQueueService;

    @PostMapping("/{callControlId}/action/leave_queue")
    public TelnyxLeaveQueueResponseDTO removeFromQueue(
            @PathVariable String callControlId,
            @RequestBody TelnyxLeaveQueueRequestDTO requestDTO) {
        return leaveQueueService.removeCallFromQueue(callControlId, requestDTO);
    }
}
