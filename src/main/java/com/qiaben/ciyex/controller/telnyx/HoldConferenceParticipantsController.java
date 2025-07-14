package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.HoldConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.HoldConferenceParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.HoldConferenceParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class HoldConferenceParticipantsController {

    private final HoldConferenceParticipantsService holdService;

    @PostMapping("/{conferenceId}/actions/hold")
    public HoldConferenceParticipantsResponseDto holdParticipants(
            @PathVariable String conferenceId,
            @RequestBody HoldConferenceParticipantsRequestDto request) {
        return holdService.holdParticipants(conferenceId, request);
    }
}
