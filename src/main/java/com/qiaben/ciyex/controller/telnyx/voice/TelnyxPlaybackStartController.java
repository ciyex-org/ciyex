package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPlaybackStartRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxPlaybackStartResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxPlaybackStartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/calls")
@RequiredArgsConstructor
public class TelnyxPlaybackStartController {

    private final TelnyxPlaybackStartService playbackStartService;

    @PostMapping("/{callControlId}/actions/playback_start")
    public ResponseEntity<TelnyxPlaybackStartResponseDTO> playAudio(
            @PathVariable String callControlId,
            @RequestBody TelnyxPlaybackStartRequestDTO requestDTO) {
        TelnyxPlaybackStartResponseDTO response = playbackStartService.playAudio(callControlId, requestDTO);
        return ResponseEntity.ok(response);
    }
}
