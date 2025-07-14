package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferencePlayAudioRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferencePlayAudioResponseDto;
import com.qiaben.ciyex.service.telnyx.ConferencePlayAudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class ConferencePlayAudioController {

    private final ConferencePlayAudioService playService;

    @PostMapping("/{conferenceId}/actions/play")
    public ConferencePlayAudioResponseDto playAudio(
            @PathVariable String conferenceId,
            @RequestBody ConferencePlayAudioRequestDto request) {
        return playService.playAudio(conferenceId, request);
    }
}
