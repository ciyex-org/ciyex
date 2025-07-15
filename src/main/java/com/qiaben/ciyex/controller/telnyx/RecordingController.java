package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RecordingCommandRequestDTO;
import com.qiaben.ciyex.dto.telnyx.RecordingCommandResponseDTO;
import com.qiaben.ciyex.service.telnyx.RecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls/{callControlId}/recording")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingService recordingService;

    @PostMapping("/start")
    public ResponseEntity<RecordingCommandResponseDTO> start(
            @PathVariable String callControlId,
            @RequestBody RecordingCommandRequestDTO body) {
        return ResponseEntity.ok(recordingService.startRecording(callControlId, body));
    }

    @PostMapping("/pause")
    public ResponseEntity<RecordingCommandResponseDTO> pause(
            @PathVariable String callControlId,
            @RequestBody RecordingCommandRequestDTO body) {
        return ResponseEntity.ok(recordingService.pauseRecording(callControlId, body));
    }

    @PostMapping("/resume")
    public ResponseEntity<RecordingCommandResponseDTO> resume(
            @PathVariable String callControlId,
            @RequestBody RecordingCommandRequestDTO body) {
        return ResponseEntity.ok(recordingService.resumeRecording(callControlId, body));
    }

    @PostMapping("/stop")
    public ResponseEntity<RecordingCommandResponseDTO> stop(
            @PathVariable String callControlId,
            @RequestBody RecordingCommandRequestDTO body) {
        return ResponseEntity.ok(recordingService.stopRecording(callControlId, body));
    }
}
