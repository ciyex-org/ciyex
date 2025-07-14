package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.LeaveConferenceRequestDto;
import com.qiaben.ciyex.dto.telnyx.LeaveConferenceResponseDto;
import com.qiaben.ciyex.service.telnyx.LeaveConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class LeaveConferenceController {

    private final LeaveConferenceService leaveConferenceService;

    @PostMapping("/{conferenceId}/actions/leave")
    public LeaveConferenceResponseDto leaveConference(
            @PathVariable String conferenceId,
            @RequestBody LeaveConferenceRequestDto request) {
        return leaveConferenceService.leaveConference(conferenceId, request);
    }
}
