package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceRecordingResumeRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceRecordingResumeResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferenceRecordingResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferenceRecordingResumeController {

    private final TelnyxConferenceRecordingResumeService resumeService;

    @PostMapping("/{conferenceId}/actions/record_resume")
    public TelnyxConferenceRecordingResumeResponseDto resumeRecording(
            @PathVariable String conferenceId,
            @RequestBody TelnyxConferenceRecordingResumeRequestDto request) {
        return resumeService.resumeRecording(conferenceId, request);
    }
}
