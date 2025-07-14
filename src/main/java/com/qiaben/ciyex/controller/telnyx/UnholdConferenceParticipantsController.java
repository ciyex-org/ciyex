package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.UnholdConferenceParticipantsRequestDto;
import com.qiaben.ciyex.dto.telnyx.UnholdConferenceParticipantsResponseDto;
import com.qiaben.ciyex.service.telnyx.UnholdConferenceParticipantsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class UnholdConferenceParticipantsController {

    private final UnholdConferenceParticipantsService unholdService;

    @PostMapping("/{conferenceId}/actions/unhold")
    public UnholdConferenceParticipantsResponseDto unholdParticipants(
            @PathVariable String conferenceId,
            @RequestBody UnholdConferenceParticipantsRequestDto request) {
        return unholdService.unholdParticipants(conferenceId, request);
    }
}
