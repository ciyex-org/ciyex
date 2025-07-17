package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceAudioStopRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceAudioStopResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferenceAudioStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferenceAudioStopController {

    private final TelnyxConferenceAudioStopService stopService;

    @PostMapping("/{conferenceId}/actions/stop")
    public TelnyxConferenceAudioStopResponseDto stopAudio(
            @PathVariable String conferenceId,
            @RequestBody TelnyxConferenceAudioStopRequestDto request) {
        return stopService.stopAudio(conferenceId, request);
    }
}
