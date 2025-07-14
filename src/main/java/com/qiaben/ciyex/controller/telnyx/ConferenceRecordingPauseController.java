package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingPauseRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingPauseResponseDto;
import com.qiaben.ciyex.service.telnyx.ConferenceRecordingPauseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class ConferenceRecordingPauseController {

    private final ConferenceRecordingPauseService pauseService;

    @PostMapping("/{conferenceId}/actions/record_pause")
    public ConferenceRecordingPauseResponseDto pauseRecording(
            @PathVariable String conferenceId,
            @RequestBody ConferenceRecordingPauseRequestDto request) {
        return pauseService.pauseRecording(conferenceId, request);
    }
}
