package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateConferenceParticipantRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxUpdateConferenceParticipantResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxUpdateConferenceParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxUpdateConferenceParticipantController {

    private final TelnyxUpdateConferenceParticipantService updateService;

    @PostMapping("/{conferenceId}/actions/update")
    public TelnyxUpdateConferenceParticipantResponseDto updateParticipant(
            @PathVariable String conferenceId,
            @RequestBody TelnyxUpdateConferenceParticipantRequestDto request) {
        return updateService.updateParticipant(conferenceId, request);
    }
}
