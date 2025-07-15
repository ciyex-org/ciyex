package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxGatherSpeakRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxGatherSpeakResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxGatherSpeakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/gather-speak")
@RequiredArgsConstructor
public class TelnyxGatherSpeakController {

    private final TelnyxGatherSpeakService gatherSpeakService;

    @PostMapping("/{callControlId}")
    public ResponseEntity<TelnyxGatherSpeakResponseDTO> gatherUsingSpeak(
            @PathVariable String callControlId,
            @RequestBody TelnyxGatherSpeakRequestDTO request
    ) {
        return ResponseEntity.ok(gatherSpeakService.gatherUsingSpeak(callControlId, request));
    }
}
