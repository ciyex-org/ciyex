package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGenericTelnyxResponseDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSendDtmfRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSendSipInfoRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxSpeakTextRequestDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCallControlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls/{callControlId}")
@RequiredArgsConstructor
public class TelnyxCallControlController {

    private final TelnyxCallControlService service;

    @PostMapping("/dtmf")
    public ResponseEntity<TelnyxGenericTelnyxResponseDTO> sendDtmf(
            @PathVariable String callControlId,
            @RequestBody TelnyxSendDtmfRequestDTO dto) {
        return ResponseEntity.ok(service.sendDtmf(callControlId, dto));
    }

    @PostMapping("/sip-info")
    public ResponseEntity<TelnyxGenericTelnyxResponseDTO> sendSipInfo(
            @PathVariable String callControlId,
            @RequestBody TelnyxSendSipInfoRequestDTO dto) {
        return ResponseEntity.ok(service.sendSipInfo(callControlId, dto));
    }

    @PostMapping("/speak")
    public ResponseEntity<TelnyxGenericTelnyxResponseDTO> speakText(
            @PathVariable String callControlId,
            @RequestBody TelnyxSpeakTextRequestDTO dto) {
        return ResponseEntity.ok(service.speakText(callControlId, dto));
    }
}

