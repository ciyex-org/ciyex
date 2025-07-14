package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.UnmuteConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.UnmuteConferenceParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.UnmuteConferenceParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class UnmuteConferenceParticipantsController {

    private final UnmuteConferenceParticipantsService unmuteService;

    @PostMapping("/{conferenceId}/actions/unmute")
    public UnmuteConferenceParticipantsResponseDto unmuteParticipants(
            @PathVariable String conferenceId,
            @RequestBody UnmuteConferenceParticipantsRequestDto request) {
        return unmuteService.unmuteParticipants(conferenceId, request);
    }
}
