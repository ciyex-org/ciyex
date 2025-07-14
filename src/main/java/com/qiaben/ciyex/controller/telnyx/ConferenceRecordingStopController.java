package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingStopRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceRecordingStopResponseDto;
import com.qiaben.ciyex.service.telnyx.ConferenceRecordingStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class ConferenceRecordingStopController {

    private final ConferenceRecordingStopService recordingStopService;

    @PostMapping("/{conferenceId}/actions/record_stop")
    public ConferenceRecordingStopResponseDto stopRecording(
            @PathVariable String conferenceId,
            @RequestBody ConferenceRecordingStopRequestDto request) {
        return recordingStopService.stopRecording(conferenceId, request);
    }
}
