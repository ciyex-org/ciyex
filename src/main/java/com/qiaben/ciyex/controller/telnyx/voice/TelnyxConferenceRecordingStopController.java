package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceRecordingStopRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceRecordingStopResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferenceRecordingStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferenceRecordingStopController {

    private final TelnyxConferenceRecordingStopService recordingStopService;

    @PostMapping("/{conferenceId}/actions/record_stop")
    public TelnyxConferenceRecordingStopResponseDto stopRecording(
            @PathVariable String conferenceId,
            @RequestBody TelnyxConferenceRecordingStopRequestDto request) {
        return recordingStopService.stopRecording(conferenceId, request);
    }
}
