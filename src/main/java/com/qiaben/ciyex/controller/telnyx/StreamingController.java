package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.StartStreamingRequestDTO;
import com.qiaben.ciyex.dto.telnyx.StopStreamingRequestDTO;
import com.qiaben.ciyex.service.telnyx.StreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/streaming")
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService streamingService;

    @PostMapping("/{callControlId}/start")
    public ResponseEntity<String> startStreaming(
            @PathVariable String callControlId,
            @RequestBody StartStreamingRequestDTO dto) {
        String response = streamingService.startStreaming(callControlId, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{callControlId}/stop")
    public ResponseEntity<String> stopStreaming(
            @PathVariable String callControlId,
            @RequestBody StopStreamingRequestDTO dto) {
        String response = streamingService.stopStreaming(callControlId, dto);
        return ResponseEntity.ok(response);
    }
}
