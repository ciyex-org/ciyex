package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferencePlayAudioRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferencePlayAudioResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferencePlayAudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferencePlayAudioController {

    private final TelnyxConferencePlayAudioService playService;

    @PostMapping("/{conferenceId}/actions/play")
    public TelnyxConferencePlayAudioResponseDto playAudio(
            @PathVariable String conferenceId,
            @RequestBody TelnyxConferencePlayAudioRequestDto request) {
        return playService.playAudio(conferenceId, request);
    }
}
