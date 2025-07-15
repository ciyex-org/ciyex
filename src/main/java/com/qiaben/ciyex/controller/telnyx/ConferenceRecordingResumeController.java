package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingResumeRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingResumeResponseDto;
import com.qiaben.ciyex.service.telnyx.ConferenceRecordingResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class ConferenceRecordingResumeController {

    private final ConferenceRecordingResumeService resumeService;

    @PostMapping("/{conferenceId}/actions/record_resume")
    public ConferenceRecordingResumeResponseDto resumeRecording(
            @PathVariable String conferenceId,
            @RequestBody ConferenceRecordingResumeRequestDto request) {
        return resumeService.resumeRecording(conferenceId, request);
    }
}
