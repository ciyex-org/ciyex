package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferenceAudioStopRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceAudioStopResponseDto;
import com.qiaben.ciyex.service.telnyx.ConferenceAudioStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class ConferenceAudioStopController {

    private final ConferenceAudioStopService stopService;

    @PostMapping("/{conferenceId}/actions/stop")
    public ConferenceAudioStopResponseDto stopAudio(
            @PathVariable String conferenceId,
            @RequestBody ConferenceAudioStopRequestDto request) {
        return stopService.stopAudio(conferenceId, request);
    }
}
