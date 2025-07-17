package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxRecordingCommandRequestDTO;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRecordingCommandResponseDTO;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls/{callControlId}/recording")
@RequiredArgsConstructor
public class TelnyxRecordingController {

    private final TelnyxRecordingService recordingService;

    @PostMapping("/start")
    public ResponseEntity<TelnyxRecordingCommandResponseDTO> start(
            @PathVariable String callControlId,
            @RequestBody TelnyxRecordingCommandRequestDTO body) {
        return ResponseEntity.ok(recordingService.startRecording(callControlId, body));
    }

    @PostMapping("/pause")
    public ResponseEntity<TelnyxRecordingCommandResponseDTO> pause(
            @PathVariable String callControlId,
            @RequestBody TelnyxRecordingCommandRequestDTO body) {
        return ResponseEntity.ok(recordingService.pauseRecording(callControlId, body));
    }

    @PostMapping("/resume")
    public ResponseEntity<TelnyxRecordingCommandResponseDTO> resume(
            @PathVariable String callControlId,
            @RequestBody TelnyxRecordingCommandRequestDTO body) {
        return ResponseEntity.ok(recordingService.resumeRecording(callControlId, body));
    }

    @PostMapping("/stop")
    public ResponseEntity<TelnyxRecordingCommandResponseDTO> stop(
            @PathVariable String callControlId,
            @RequestBody TelnyxRecordingCommandRequestDTO body) {
        return ResponseEntity.ok(recordingService.stopRecording(callControlId, body));
    }
}
