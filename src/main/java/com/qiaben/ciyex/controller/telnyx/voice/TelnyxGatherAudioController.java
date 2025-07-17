package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherAudioRequestDTO;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxGatherAudioResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxGatherAudioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/gather-audio")
@RequiredArgsConstructor
public class TelnyxGatherAudioController {

    private final TelnyxGatherAudioService gatherAudioService;

    @PostMapping("/{callControlId}")
    public ResponseEntity<TelnyxGatherAudioResponseDTO> gatherUsingAudio(
            @PathVariable String callControlId,
            @RequestBody TelnyxGatherAudioRequestDTO request
    ) {
        return ResponseEntity.ok(gatherAudioService.gatherUsingAudio(callControlId, request));
    }
}
