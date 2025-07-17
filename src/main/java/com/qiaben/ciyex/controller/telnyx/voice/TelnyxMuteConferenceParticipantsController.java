package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxMuteConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxMuteConferenceParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxMuteConferenceParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxMuteConferenceParticipantsController {

    private final TelnyxMuteConferenceParticipantsService muteService;

    @PostMapping("/{conferenceId}/actions/mute")
    public TelnyxMuteConferenceParticipantsResponseDto muteParticipants(
            @PathVariable String conferenceId,
            @RequestBody TelnyxMuteConferenceParticipantsRequestDto request) {
        return muteService.muteParticipants(conferenceId, request);
    }
}
