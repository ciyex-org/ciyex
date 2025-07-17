package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceSpeakRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceSpeakResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferenceSpeakService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferenceSpeakController {

    private final TelnyxConferenceSpeakService speakService;

    @PostMapping("/{conferenceId}/actions/speak")
    public TelnyxConferenceSpeakResponseDto speakToParticipants(
            @PathVariable String conferenceId,
            @RequestBody TelnyxConferenceSpeakRequestDto request) {
        return speakService.speak(conferenceId, request);
    }
}
