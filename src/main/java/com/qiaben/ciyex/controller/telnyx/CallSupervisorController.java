package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.CallSupervisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/call-control")
@RequiredArgsConstructor
public class CallSupervisorController {

    private final CallSupervisorService service;

    @PostMapping("/{callId}/switch-role")
    public ResponseEntity<String> switchRole(
            @PathVariable String callId,
            @RequestBody SupervisorRoleSwitchDTO dto) {
        return ResponseEntity.ok(service.switchRole(callId, dto));
    }

    @PostMapping("/{callId}/transcription/start")
    public ResponseEntity<String> startTranscription(
            @PathVariable String callId,
            @RequestBody TranscriptionStartDTO dto) {
        return ResponseEntity.ok(service.startTranscription(callId, dto));
    }

    @PostMapping("/{callId}/transcription/stop")
    public ResponseEntity<String> stopTranscription(
            @PathVariable String callId,
            @RequestBody TranscriptionStopDTO dto) {
        return ResponseEntity.ok(service.stopTranscription(callId, dto));
    }

    @PostMapping("/{callId}/transfer")
    public ResponseEntity<String> transferCall(
            @PathVariable String callId,
            @RequestBody TransferCallDTO dto) {
        return ResponseEntity.ok(service.transferCall(callId, dto));
    }
}
