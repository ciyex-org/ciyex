package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxStartStreamingRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxStopStreamingRequestDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxStreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/streaming")
@RequiredArgsConstructor
public class TelnyxStreamingController {

    private final TelnyxStreamingService telnyxStreamingService;

    @PostMapping("/{callControlId}/start")
    public ResponseEntity<String> startStreaming(
            @PathVariable String callControlId,
            @RequestBody TelnyxStartStreamingRequestDTO dto) {
        String response = telnyxStreamingService.startStreaming(callControlId, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{callControlId}/stop")
    public ResponseEntity<String> stopStreaming(
            @PathVariable String callControlId,
            @RequestBody TelnyxStopStreamingRequestDTO dto) {
        String response = telnyxStreamingService.stopStreaming(callControlId, dto);
        return ResponseEntity.ok(response);
    }
}
