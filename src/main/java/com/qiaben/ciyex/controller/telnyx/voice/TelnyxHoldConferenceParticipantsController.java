package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxHoldConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxHoldConferenceParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxHoldConferenceParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxHoldConferenceParticipantsController {

    private final TelnyxHoldConferenceParticipantsService holdService;

    @PostMapping("/{conferenceId}/actions/hold")
    public TelnyxHoldConferenceParticipantsResponseDto holdParticipants(
            @PathVariable String conferenceId,
            @RequestBody TelnyxHoldConferenceParticipantsRequestDto request) {
        return holdService.holdParticipants(conferenceId, request);
    }
}
