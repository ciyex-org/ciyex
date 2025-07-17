package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUnholdConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUnholdConferenceParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxUnholdConferenceParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxUnholdConferenceParticipantsController {

    private final TelnyxUnholdConferenceParticipantsService unholdService;

    @PostMapping("/{conferenceId}/actions/unhold")
    public TelnyxUnholdConferenceParticipantsResponseDto unholdParticipants(
            @PathVariable String conferenceId,
            @RequestBody TelnyxUnholdConferenceParticipantsRequestDto request) {
        return unholdService.unholdParticipants(conferenceId, request);
    }
}
