package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.MuteConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.MuteConferenceParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.MuteConferenceParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class MuteConferenceParticipantsController {

    private final MuteConferenceParticipantsService muteService;

    @PostMapping("/{conferenceId}/actions/mute")
    public MuteConferenceParticipantsResponseDto muteParticipants(
            @PathVariable String conferenceId,
            @RequestBody MuteConferenceParticipantsRequestDto request) {
        return muteService.muteParticipants(conferenceId, request);
    }
}
