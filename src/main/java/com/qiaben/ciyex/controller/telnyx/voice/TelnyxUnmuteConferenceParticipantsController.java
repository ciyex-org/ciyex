package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUnmuteConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUnmuteConferenceParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxUnmuteConferenceParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxUnmuteConferenceParticipantsController {

    private final TelnyxUnmuteConferenceParticipantsService unmuteService;

    @PostMapping("/{conferenceId}/actions/unmute")
    public TelnyxUnmuteConferenceParticipantsResponseDto unmuteParticipants(
            @PathVariable String conferenceId,
            @RequestBody TelnyxUnmuteConferenceParticipantsRequestDto request) {
        return unmuteService.unmuteParticipants(conferenceId, request);
    }
}
