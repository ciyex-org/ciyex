package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.CallControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls/{callControlId}")
@RequiredArgsConstructor
public class CallControlController {

    private final CallControlService service;

    @PostMapping("/dtmf")
    public ResponseEntity<GenericTelnyxResponseDTO> sendDtmf(
            @PathVariable String callControlId,
            @RequestBody SendDtmfRequestDTO dto) {
        return ResponseEntity.ok(service.sendDtmf(callControlId, dto));
    }

    @PostMapping("/sip-info")
    public ResponseEntity<GenericTelnyxResponseDTO> sendSipInfo(
            @PathVariable String callControlId,
            @RequestBody SendSipInfoRequestDTO dto) {
        return ResponseEntity.ok(service.sendSipInfo(callControlId, dto));
    }

    @PostMapping("/speak")
    public ResponseEntity<GenericTelnyxResponseDTO> speakText(
            @PathVariable String callControlId,
            @RequestBody SpeakTextRequestDTO dto) {
        return ResponseEntity.ok(service.speakText(callControlId, dto));
    }
}

