package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSupervisorRoleSwitchDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTranscriptionStartDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTranscriptionStopDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxTransferCallDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCallSupervisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/call-control")
@RequiredArgsConstructor
public class TelnyxCallSupervisorController {

    private final TelnyxCallSupervisorService service;

    @PostMapping("/{callId}/switch-role")
    public ResponseEntity<String> switchRole(
            @PathVariable String callId,
            @RequestBody TelnyxSupervisorRoleSwitchDTO dto) {
        return ResponseEntity.ok(service.switchRole(callId, dto));
    }

    @PostMapping("/{callId}/transcription/start")
    public ResponseEntity<String> startTranscription(
            @PathVariable String callId,
            @RequestBody TelnyxTranscriptionStartDTO dto) {
        return ResponseEntity.ok(service.startTranscription(callId, dto));
    }

    @PostMapping("/{callId}/transcription/stop")
    public ResponseEntity<String> stopTranscription(
            @PathVariable String callId,
            @RequestBody TelnyxTranscriptionStopDTO dto) {
        return ResponseEntity.ok(service.stopTranscription(callId, dto));
    }

    @PostMapping("/{callId}/transfer")
    public ResponseEntity<String> transferCall(
            @PathVariable String callId,
            @RequestBody TelnyxTransferCallDTO dto) {
        return ResponseEntity.ok(service.transferCall(callId, dto));
    }
}
