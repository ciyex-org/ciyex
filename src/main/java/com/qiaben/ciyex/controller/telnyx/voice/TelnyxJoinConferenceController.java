package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxJoinConferenceRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxJoinConferenceResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxJoinConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxJoinConferenceController {

    private final TelnyxJoinConferenceService telnyxJoinConferenceService;

    @PostMapping("/{conferenceId}/actions/join")
    public TelnyxJoinConferenceResponseDto joinConference(
            @PathVariable String conferenceId,
            @RequestBody TelnyxJoinConferenceRequestDto request) {
        return telnyxJoinConferenceService.joinConference(conferenceId, request);
    }
}
