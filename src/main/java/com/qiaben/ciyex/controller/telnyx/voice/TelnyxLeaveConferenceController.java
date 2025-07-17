package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxLeaveConferenceRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxLeaveConferenceResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxLeaveConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxLeaveConferenceController {

    private final TelnyxLeaveConferenceService telnyxLeaveConferenceService;

    @PostMapping("/{conferenceId}/actions/leave")
    public TelnyxLeaveConferenceResponseDto leaveConference(
            @PathVariable String conferenceId,
            @RequestBody TelnyxLeaveConferenceRequestDto request) {
        return telnyxLeaveConferenceService.leaveConference(conferenceId, request);
    }
}
