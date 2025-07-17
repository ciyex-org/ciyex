package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxNoiseSuppressionStartRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxNoiseSuppressionStopRequestDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxNoiseSuppressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/noise-suppression")
@RequiredArgsConstructor
public class TelnyxNoiseSuppressionController {

    private final TelnyxNoiseSuppressionService service;

    @PostMapping("/{callControlId}/start")
    public ResponseEntity<String> start(
            @PathVariable String callControlId,
            @RequestBody TelnyxNoiseSuppressionStartRequestDTO dto) {

        return ResponseEntity.ok(service.start(callControlId, dto));
    }

    @PostMapping("/{callControlId}/stop")
    public ResponseEntity<String> stop(
            @PathVariable String callControlId,
            @RequestBody TelnyxNoiseSuppressionStopRequestDTO dto) {

        return ResponseEntity.ok(service.stop(callControlId, dto));
    }
}

