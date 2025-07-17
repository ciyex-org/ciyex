package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceRecordingPauseRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceRecordingPauseResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferenceRecordingPauseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferenceRecordingPauseController {

    private final TelnyxConferenceRecordingPauseService pauseService;

    @PostMapping("/{conferenceId}/actions/record_pause")
    public TelnyxConferenceRecordingPauseResponseDto pauseRecording(
            @PathVariable String conferenceId,
            @RequestBody TelnyxConferenceRecordingPauseRequestDto request) {
        return pauseService.pauseRecording(conferenceId, request);
    }
}
