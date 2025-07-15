package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxPlaybackStopRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxPlaybackStopResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxPlaybackStopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls")
@RequiredArgsConstructor
public class TelnyxPlaybackStopController {

    private final TelnyxPlaybackStopService playbackStopService;

    @PostMapping("/{callControlId}/actions/playback_stop")
    public ResponseEntity<TelnyxPlaybackStopResponseDTO> stopPlayback(
            @PathVariable String callControlId,
            @RequestBody TelnyxPlaybackStopRequestDTO requestDTO) {
        TelnyxPlaybackStopResponseDTO response = playbackStopService.stopPlayback(callControlId, requestDTO);
        return ResponseEntity.ok(response);
    }
}
