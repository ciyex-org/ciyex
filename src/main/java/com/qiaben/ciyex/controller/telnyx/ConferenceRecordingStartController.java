package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingStartRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingStartResponseDto;
import com.qiaben.ciyex.service.telnyx.ConferenceRecordingStartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class ConferenceRecordingStartController {

    private final ConferenceRecordingStartService recordingStartService;

    @PostMapping("/{conferenceId}/actions/record_start")
    public ConferenceRecordingStartResponseDto startRecording(
            @PathVariable String conferenceId,
            @RequestBody ConferenceRecordingStartRequestDto request) {
        return recordingStartService.startRecording(conferenceId, request);
    }
}
