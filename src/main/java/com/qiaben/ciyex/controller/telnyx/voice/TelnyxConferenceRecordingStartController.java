package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceRecordingStartRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceRecordingStartResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferenceRecordingStartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferenceRecordingStartController {

    private final TelnyxConferenceRecordingStartService recordingStartService;

    @PostMapping("/{conferenceId}/actions/record_start")
    public TelnyxConferenceRecordingStartResponseDto startRecording(
            @PathVariable String conferenceId,
            @RequestBody TelnyxConferenceRecordingStartRequestDto request) {
        return recordingStartService.startRecording(conferenceId, request);
    }
}
